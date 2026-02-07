package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.iam.auth.api.dto.request.RegisterRequest;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.otp.adapter.service.OtpService;
import az.fitnest.iam.otp.api.dto.request.OtpSendRequest;
import az.fitnest.iam.otp.api.dto.response.OtpSendResponse;
import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import az.fitnest.iam.otp.domain.model.OtpVerificationResult;
import az.fitnest.iam.shared.exception.InvalidCredentialsException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.domain.model.User;
import az.fitnest.iam.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserService userService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final TokenIssuanceService tokenIssuanceService;
    private final OtpService otpService;

    @Transactional
    public OtpSendResponse startRegistration(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        
        String passwordHash = passwordService.hashPassword(request.getPassword());
        
        OtpSendRequest otpRequest = OtpSendRequest.builder()
                .email(request.getEmail())
                .purpose(OtpPurpose.REGISTRATION)
                .build();
        
        return otpService.sendOtp(
                otpRequest, 
                request.getFirstName(), 
                request.getLastName(), 
                passwordHash
        );
    }

    @Transactional
    public LoginResponse completeRegistration(RegisterCompleteRequest request) {
        OtpVerificationResult result = otpService.verifyOtpByEmail(
                request.getEmail(), 
                OtpPurpose.REGISTRATION, 
                request.getOtpCode()
        );
        
        if (result.getPurpose() != OtpPurpose.REGISTRATION) {
            throw new InvalidCredentialsException("Invalid OTP purpose");
        }

        User user = userService.createNewUser(
                result.getEmail(),
                result.getFirstName(),
                result.getLastName(),
                result.getPasswordHash()
        );

        return tokenIssuanceService.issueTokens(user);
    }

    private LoginResponse loginAfterRegistration(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (user.getPasswordHash() == null) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!passwordService.verifyPassword(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return tokenIssuanceService.issueTokens(user);
    }
}
