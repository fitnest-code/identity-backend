package az.fitnest.iam.setup.adapter.service;

import az.fitnest.iam.setup.api.dto.response.CompleteSetupResponse;
import az.fitnest.iam.setup.api.dto.response.FitnessLevelResponse;
import az.fitnest.iam.setup.api.dto.response.GoalResponse;
import az.fitnest.iam.setup.api.dto.response.GoalsResponse;
import az.fitnest.iam.setup.api.dto.response.ProfileResponse;
import az.fitnest.iam.setup.api.dto.response.SetupStatusResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class SetupMapper {

    public ProfileResponse toProfileResponse(az.fitnest.iam.setup.adapter.client.dto.ProfileResponse client) {
        return mapOrNull(client, c -> ProfileResponse.builder()
                .profile(mapProfile(c.getProfile()))
                .build());
    }

    public ProfileResponse.ProfileData mapProfile(az.fitnest.iam.setup.adapter.client.dto.ProfileResponse.ProfileData client) {
        return mapOrNull(client, c -> toProfileResponseData(mapProfileFields(
                c,
                az.fitnest.iam.setup.adapter.client.dto.ProfileResponse.ProfileData::getHeightCm,
                az.fitnest.iam.setup.adapter.client.dto.ProfileResponse.ProfileData::getWeightKg,
                az.fitnest.iam.setup.adapter.client.dto.ProfileResponse.ProfileData::getGender,
                az.fitnest.iam.setup.adapter.client.dto.ProfileResponse.ProfileData::getAge
        )));
    }

    public GoalResponse toGoalResponse(az.fitnest.iam.setup.adapter.client.dto.GoalResponse client) {
        return mapOrNull(client, c -> GoalResponse.builder()
                .goal(c.getGoal())
                .build());
    }

    public FitnessLevelResponse toFitnessLevelResponse(az.fitnest.iam.setup.adapter.client.dto.FitnessLevelResponse client) {
        return mapOrNull(client, c -> FitnessLevelResponse.builder()
                .bmi(c.getBmi())
                .bmiCategory(c.getBmiCategory())
                .bmiScale(mapBmiScale(c.getBmiScale()))
                .goal(c.getGoal())
                .message(c.getMessage())
                .build());
    }

    private FitnessLevelResponse.BmiScale mapBmiScale(az.fitnest.iam.setup.adapter.client.dto.FitnessLevelResponse.BmiScale client) {
        return mapOrNull(client, c -> FitnessLevelResponse.BmiScale.builder()
                .underweightMax(c.getUnderweightMax())
                .normalMax(c.getNormalMax())
                .overweightMax(c.getOverweightMax())
                .build());
    }

    public CompleteSetupResponse toCompleteSetupResponse(az.fitnest.iam.setup.adapter.client.dto.CompleteSetupResponse client) {
        return mapOrNull(client, c -> CompleteSetupResponse.builder()
                .setupRequired(c.getSetupRequired())
                .next(mapNextSteps(c.getNext()))
                .build());
    }

    private CompleteSetupResponse.NextSteps mapNextSteps(az.fitnest.iam.setup.adapter.client.dto.CompleteSetupResponse.NextSteps client) {
        return mapOrNull(client, c -> CompleteSetupResponse.NextSteps.builder()
                .workoutPlanReady(c.getWorkoutPlanReady())
                .nutritionPlanReady(c.getNutritionPlanReady())
                .build());
    }

    public SetupStatusResponse toSetupStatusResponse(
            Boolean setupRequired,
            az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse client) {
        if (client == null) {
            return SetupStatusResponse.builder()
                    .setupRequired(setupRequired)
                    .build();
        }
        return SetupStatusResponse.builder()
                .setupRequired(setupRequired)
                .profile(toProfileData(client.getProfile()))
                .goal(client.getGoal())
                .build();
    }

    public SetupStatusResponse.ProfileData toProfileData(az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse.ProfileData client) {
        return mapOrNull(client, c -> toSetupStatusProfileData(mapProfileFields(
                c,
                az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse.ProfileData::getHeightCm,
                az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse.ProfileData::getWeightKg,
                az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse.ProfileData::getGender,
                az.fitnest.iam.setup.adapter.client.dto.SetupStatusResponse.ProfileData::getAge
        )));
    }

    public GoalsResponse mapGoals(az.fitnest.iam.setup.adapter.client.dto.GoalsResponse client) {
        if (client == null || client.getItems() == null) return null;
        List<GoalsResponse.GoalItem> items = client.getItems().stream()
                .map(i -> mapOrNull(i, this::toGoalItem))
                .toList();
        
        return GoalsResponse.builder()
                .items(items)
                .build();
    }

    private GoalsResponse.GoalItem toGoalItem(az.fitnest.iam.setup.adapter.client.dto.GoalsResponse.GoalItem client) {
        return GoalsResponse.GoalItem.builder()
                .code(client.getCode())
                .title(client.getTitle())
                .subtitle(client.getSubtitle())
                .build();
    }

    private <T, R> R mapOrNull(T source, Function<T, R> mapper) {
        return source == null ? null : mapper.apply(source);
    }

    private record ProfileFields(Integer heightCm, Double weightKg, String gender, Integer age) {
    }

    private <T> ProfileFields mapProfileFields(
            T source,
            Function<T, Integer> heightCmMapper,
            Function<T, Double> weightKgMapper,
            Function<T, String> genderMapper,
            Function<T, Integer> ageMapper
    ) {
        return new ProfileFields(
                heightCmMapper.apply(source),
                weightKgMapper.apply(source),
                genderMapper.apply(source),
                ageMapper.apply(source)
        );
    }

    private ProfileResponse.ProfileData toProfileResponseData(ProfileFields f) {
        return ProfileResponse.ProfileData.builder()
                .heightCm(f.heightCm())
                .weightKg(f.weightKg())
                .gender(f.gender())
                .age(f.age())
                .build();
    }

    private SetupStatusResponse.ProfileData toSetupStatusProfileData(ProfileFields f) {
        return SetupStatusResponse.ProfileData.builder()
                .heightCm(f.heightCm())
                .weightKg(f.weightKg())
                .gender(f.gender())
                .age(f.age())
                .build();
    }
}
