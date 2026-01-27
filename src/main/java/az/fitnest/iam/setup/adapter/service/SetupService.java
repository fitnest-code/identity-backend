package az.fitnest.iam.setup.adapter.service;

import az.fitnest.iam.setup.api.dto.request.UpdateProfileRequest;
import az.fitnest.iam.setup.api.dto.response.*;
import az.fitnest.iam.setup.adapter.client.UserServiceClient;
import az.fitnest.iam.shared.exception.BadRequestException;
import az.fitnest.iam.shared.exception.ResourceNotFoundException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.domain.model.User;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SetupService {

    private final UserRepository userRepository;
    private final UserServiceClient userServiceClient;
    private final SetupMapper mapper;

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public SetupStatusResponse getSetupStatus(Long userId) {
        User user = getUserOrThrow(userId);
        az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse clientResponse = 
                userServiceClient.getSetupStatus(String.valueOf(userId));
        
        return mapper.toSetupStatusResponse(user.getSetupRequired(), clientResponse);
    }

    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        getUserOrThrow(userId);

        az.fitnest.iam.setup.adapter.client.dto.UpdateProfileRequest clientRequest = 
                az.fitnest.iam.setup.adapter.client.dto.UpdateProfileRequest.builder()
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .gender(request.getGender())
                .age(request.getAge())
                .build();

        az.fitnest.iam.setup.adapter.client.dto.ProfileResponse clientResponse = 
                userServiceClient.updateProfile(String.valueOf(userId), clientRequest);
        
        return mapper.toProfileResponse(clientResponse);
    }

    public GoalsResponse getReferenceGoals() {
        try {
            az.fitnest.iam.setup.adapter.client.dto.GoalsResponse clientResponse = 
                    userServiceClient.getReferenceGoals();
            return mapper.mapGoals(clientResponse);
        } catch (FeignException.ServiceUnavailable | FeignException.GatewayTimeout | FeignException.BadGateway e) {
            log.warn("User-service unavailable for goals, falling back to defaults", e);
            return getDefaultGoals();
        } catch (RestClientException e) {
            if (e.getCause() instanceof ConnectException || e.getCause() instanceof TimeoutException) {
                log.warn("User-service connection failed for goals, falling back to defaults", e);
                return getDefaultGoals();
            }
            throw e;
        }
    }

    public GoalResponse updateGoal(Long userId, String goalCode) {
        getUserOrThrow(userId);

        if (goalCode == null || goalCode.isBlank()) {
            throw new BadRequestException("Goal code is required");
        }

        az.fitnest.iam.setup.adapter.client.dto.UpdateGoalRequest request = 
                az.fitnest.iam.setup.adapter.client.dto.UpdateGoalRequest.builder()
                .goal(goalCode)
                .build();
        
        az.fitnest.iam.setup.adapter.client.dto.GoalResponse clientResponse = 
                userServiceClient.updateGoal(String.valueOf(userId), request);
        
        return mapper.toGoalResponse(clientResponse);
    }

    public FitnessLevelResponse getFitnessLevel(Long userId) {
        getUserOrThrow(userId);
        az.fitnest.iam.setup.adapter.client.dto.FitnessLevelResponse clientResponse = 
                userServiceClient.getFitnessLevel(String.valueOf(userId));
        
        return mapper.toFitnessLevelResponse(clientResponse);
    }

    @Transactional
    public CompleteSetupResponse completeSetup(Long userId) {
        User user = getUserOrThrow(userId);

        if (Boolean.FALSE.equals(user.getSetupRequired())) {
            return alreadyCompletedResponse();
        }

        az.fitnest.iam.setup.adapter.client.dto.CompleteSetupResponse clientResponse = 
                userServiceClient.completeSetup(String.valueOf(userId));
        
        user.setSetupRequired(false);
        userRepository.save(user);

        return mapper.toCompleteSetupResponse(clientResponse);
    }

    private CompleteSetupResponse alreadyCompletedResponse() {
        return CompleteSetupResponse.builder()
                .setupRequired(false)
                .next(CompleteSetupResponse.NextSteps.builder()
                        .workoutPlanReady(true)
                        .nutritionPlanReady(true)
                        .build())
                .build();
    }


    private GoalsResponse getDefaultGoals() {
        List<GoalsResponse.GoalItem> items = List.of(
                GoalsResponse.GoalItem.builder()
                        .code("muscle_gain")
                        .title("Əzələ yığmaq")
                        .subtitle("Güc və əzələ kütləsini artırmaq")
                        .build(),
                GoalsResponse.GoalItem.builder()
                        .code("health")
                        .title("Sağlamlığı qorumaq")
                        .subtitle("Enerji və davamlılığı yüksəltmək")
                        .build(),
                GoalsResponse.GoalItem.builder()
                        .code("weight_loss")
                        .title("Çəki azaltmaq")
                        .subtitle("Çəki azaldaraq sağlamlaşmaq")
                        .build(),
                GoalsResponse.GoalItem.builder()
                        .code("endurance")
                        .title("Dözümlülüyü artırmaq")
                        .subtitle("Enerji və davamlılığı yüksəltmək")
                        .build(),
                GoalsResponse.GoalItem.builder()
                        .code("fit")
                        .title("Çevik və formada qalmaq")
                        .subtitle("Aktiv olmaq və özünü yaxşı hiss etmək")
                        .build()
        );

        return GoalsResponse.builder()
                .items(items)
                .build();
    }
}
