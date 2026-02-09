package az.fitnest.iam.setup.api;

import az.fitnest.iam.setup.api.dto.request.CompleteSetupRequest;
import az.fitnest.iam.setup.api.dto.request.UpdateGoalRequest;
import az.fitnest.iam.setup.api.dto.request.UpdateProfileRequest;
import az.fitnest.iam.setup.api.dto.response.*;
import az.fitnest.iam.setup.adapter.service.SetupService;
import az.fitnest.iam.shared.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Setup", description = "Endpoints for user onboarding and setup flow")
public class SetupController {

    private final SetupService setupService;

    @Operation(
            summary = "Get setup status",
            description = "Returns setup flow status and previously entered values. Mobile app selects default/active UI state based on this response."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Setup status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SetupStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - token invalid or user deactivated",
                    content = @Content
            )
    })
    @GetMapping("/setup")
    public ResponseEntity<SetupStatusResponse> getSetupStatus() {
        Long userId = UserContext.getCurrentUserId();
        SetupStatusResponse response = setupService.getSetupStatus(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update user profile",
            description = "Saves user body information: height, weight, gender, age"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = UserContext.getCurrentUserId();
        ProfileResponse response = setupService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Update user goal",
            description = "Saves user's selected goal"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Goal updated successfully",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    @PutMapping("/goal")
    public ResponseEntity<GoalResponse> updateGoal(@Valid @RequestBody UpdateGoalRequest request) {
        Long userId = UserContext.getCurrentUserId();
        GoalResponse response = setupService.updateGoal(userId, request.getGoal());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get fitness level",
            description = "Returns BMI and fitness level calculations. Mobile app displays slider and category based on this data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Fitness level calculated successfully",
                    content = @Content(schema = @Schema(implementation = FitnessLevelResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Profile incomplete - height_cm/weight_kg missing",
                    content = @Content
            )
    })
    @GetMapping("/fitness-level")
    public ResponseEntity<FitnessLevelResponse> getFitnessLevel() {
        Long userId = UserContext.getCurrentUserId();
        FitnessLevelResponse response = setupService.getFitnessLevel(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Complete setup",
            description = "Completes onboarding/setup process and sets setup_required=false. Optionally triggers plan preparation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Setup completed successfully",
                    content = @Content(schema = @Schema(implementation = CompleteSetupResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Setup incomplete - required fields missing",
                    content = @Content
            )
    })
    @PostMapping("/setup/complete")
    public ResponseEntity<CompleteSetupResponse> completeSetup(@Valid @RequestBody CompleteSetupRequest request) {
        Long userId = UserContext.getCurrentUserId();
        CompleteSetupResponse response = setupService.completeSetup(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get reference goals",
            description = "Returns list of goals for display in UI. This is reference data; caching (e.g., 24 hours) is recommended."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Goals retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GoalsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    @GetMapping("/reference/goals")
    public ResponseEntity<GoalsResponse> getReferenceGoals() {
        GoalsResponse response = setupService.getReferenceGoals();
        return ResponseEntity.ok(response);
    }
}
