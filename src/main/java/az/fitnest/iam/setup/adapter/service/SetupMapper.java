package az.fitnest.iam.setup.adapter.service;

import az.fitnest.iam.setup.adapter.client.dto.*;
import az.fitnest.iam.setup.api.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SetupMapper {

    public ProfileResponse toProfileResponse(az.fitnest.iam.setup.adapter.client.dto.ProfileResponse client) {
        if (client == null) return null;
        return ProfileResponse.builder()
                .profile(mapProfile(client.getProfile()))
                .build();
    }

    public ProfileResponse.ProfileData mapProfile(az.fitnest.iam.setup.adapter.client.dto.ProfileResponse.ProfileData client) {
        if (client == null) return null;
        return ProfileResponse.ProfileData.builder()
                .heightCm(client.getHeightCm())
                .weightKg(client.getWeightKg())
                .gender(client.getGender())
                .age(client.getAge())
                .build();
    }

    public GoalResponse toGoalResponse(az.fitnest.iam.setup.adapter.client.dto.GoalResponse client) {
        if (client == null) return null;
        return GoalResponse.builder()
                .goal(client.getGoal())
                .build();
    }

    public FitnessLevelResponse toFitnessLevelResponse(az.fitnest.iam.setup.adapter.client.dto.FitnessLevelResponse client) {
        if (client == null) return null;
        return FitnessLevelResponse.builder()
                .bmi(client.getBmi())
                .bmiCategory(client.getBmiCategory())
                .bmiScale(mapBmiScale(client.getBmiScale()))
                .goal(client.getGoal())
                .message(client.getMessage())
                .build();
    }

    private FitnessLevelResponse.BmiScale mapBmiScale(az.fitnest.iam.setup.adapter.client.dto.FitnessLevelResponse.BmiScale client) {
        if (client == null) return null;
        return FitnessLevelResponse.BmiScale.builder()
                .underweightMax(client.getUnderweightMax())
                .normalMax(client.getNormalMax())
                .overweightMax(client.getOverweightMax())
                .build();
    }

    public CompleteSetupResponse toCompleteSetupResponse(az.fitnest.iam.setup.adapter.client.dto.CompleteSetupResponse client) {
        if (client == null) return null;
        return CompleteSetupResponse.builder()
                .setupRequired(client.getSetupRequired())
                .next(mapNextSteps(client.getNext()))
                .build();
    }

    private CompleteSetupResponse.NextSteps mapNextSteps(az.fitnest.iam.setup.adapter.client.dto.CompleteSetupResponse.NextSteps client) {
        if (client == null) return null;
        return CompleteSetupResponse.NextSteps.builder()
                .workoutPlanReady(client.getWorkoutPlanReady())
                .nutritionPlanReady(client.getNutritionPlanReady())
                .build();
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
        if (client == null) return null;
        return SetupStatusResponse.ProfileData.builder()
                .heightCm(client.getHeightCm())
                .weightKg(client.getWeightKg())
                .gender(client.getGender())
                .age(client.getAge())
                .build();
    }

    public GoalsResponse mapGoals(az.fitnest.iam.setup.adapter.client.dto.GoalsResponse client) {
        if (client == null || client.getItems() == null) return null;
        List<GoalsResponse.GoalItem> items = client.getItems().stream()
                .map(this::mapGoalItem)
                .collect(Collectors.toList());
        
        return GoalsResponse.builder()
                .items(items)
                .build();
    }

    private GoalsResponse.GoalItem mapGoalItem(az.fitnest.iam.setup.adapter.client.dto.GoalsResponse.GoalItem client) {
        if (client == null) return null;
        return GoalsResponse.GoalItem.builder()
                .code(client.getCode())
                .title(client.getTitle())
                .subtitle(client.getSubtitle())
                .build();
    }
}
