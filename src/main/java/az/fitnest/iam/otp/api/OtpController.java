package az.fitnest.iam.otp.api;

import az.fitnest.iam.otp.api.dto.request.OtpSendRequest;
import az.fitnest.iam.otp.api.dto.request.OtpVerifyRequest;
import az.fitnest.iam.otp.api.dto.response.OtpSendResponse;
import az.fitnest.iam.otp.api.dto.response.OtpVerifyResponse;
import az.fitnest.iam.otp.adapter.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "Endpoints for sending and verifying OTP codes")
public class OtpController {

    private final OtpService otpService;

    @Operation(
            summary = "Send OTP",
            description = "Sends a 4-digit OTP code to the specified mobile number. " +
                    "Supports REGISTRATION and LOGIN purposes. " +
                    "Rate limiting is applied to prevent abuse. " +
                    "For REGISTRATION: mobile number must not be already registered. " +
                    "For LOGIN: mobile number must be registered."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = OtpSendResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Mobile number not found (for LOGIN purpose)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Mobile number already registered (for REGISTRATION purpose)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "OTP rate limited - too many requests",
                    content = @Content
            )
    })
    @PostMapping("/send")
    public ResponseEntity<OtpSendResponse> sendOtp(
            @Valid @RequestBody OtpSendRequest request
    ) {
        OtpSendResponse response = otpService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Verify OTP",
            description = "Verifies the OTP code provided by the user. " +
                    "On successful verification, issues a registration token that can be used to complete registration. " +
                    "Maximum 5 verification attempts allowed before session is locked."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully, registration token issued",
                    content = @Content(schema = @Schema(implementation = OtpVerifyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data, OTP session locked, OTP already verified, or too many attempts",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid OTP code",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "OTP session not found or expired",
                    content = @Content
            )
    })
    @PostMapping("/verify")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request
    ) {
        OtpVerifyResponse response = otpService.verifyOtpAndIssueToken(request);
        return ResponseEntity.ok(response);
    }
}