package az.fitnest.iam.setup.adapter.client;

import az.fitnest.iam.setup.adapter.client.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@FeignClient(
        name = "user-service",
        url = "${USER_SERVICE_URL:http://user-service:8080}",
        path = "/api/v1",
        configuration = UserServiceClientConfig.class
)
public interface UserServiceClient {

    @GetMapping("/internal/setup/status")
    SetupStatusResponse getSetupStatus(@RequestHeader(value = "X-User-Id", required = true) @NotBlank String userId);

    @PutMapping("/internal/profile")
    ProfileResponse updateProfile(
            @RequestHeader(value = "X-User-Id", required = true) @NotBlank String userId,
            @RequestBody @Valid @NotNull UpdateProfileRequest request);

    @PutMapping("/internal/goal")
    GoalResponse updateGoal(
            @RequestHeader(value = "X-User-Id", required = true) @NotBlank String userId,
            @RequestBody @Valid @NotNull UpdateGoalRequest request);

    @GetMapping("/internal/fitness-level")
    FitnessLevelResponse getFitnessLevel(@RequestHeader(value = "X-User-Id", required = true) @NotBlank String userId);

    @PostMapping("/internal/setup/complete")
    CompleteSetupResponse completeSetup(@RequestHeader(value = "X-User-Id", required = true) @NotBlank String userId);

    @GetMapping("/reference/goals")
    GoalsResponse getReferenceGoals();
}
