package az.fitnest.identity.auth.adapter.service;

import az.fitnest.identity.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.identity.auth.api.dto.request.RegisterRequest;
import az.fitnest.identity.auth.api.dto.response.LoginResponse;
import az.fitnest.identity.otp.adapter.service.OtpService;
import az.fitnest.identity.otp.api.dto.request.OtpSendRequest;
import az.fitnest.identity.otp.api.dto.response.OtpSendResponse;
import az.fitnest.identity.otp.domain.enums.OtpPurpose;
import az.fitnest.identity.otp.domain.model.OtpVerificationResult;
import az.fitnest.identity.shared.exception.InvalidCredentialsException;
import az.fitnest.identity.user.adapter.persistence.UserRepository;
import az.fitnest.identity.user.adapter.service.UserService;
import az.fitnest.identity.user.domain.model.User;
import az.fitnest.identity.shared.exception.ConflictException;
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
    private final RegistrationTokenService registrationTokenService;

    public OtpSendResponse startRegistration(RegisterRequest request) {
        String mobile = az.fitnest.identity.shared.util.MobileNumberUtils.normalize(request.getMobile());
        if (userRepository.findByMobileIncludingDeleted(mobile).isPresent()) {
            throw new ConflictException("Mobile number already registered");
        }
        
        OtpSendRequest otpRequest = OtpSendRequest.builder()
                .mobile(mobile)
                .purpose(OtpPurpose.REGISTRATION)
                .build();
        
        return otpService.sendOtp(
                otpRequest, 
                null, 
                null, 
                null, 
                mobile
        );
    }

    @Transactional
    public LoginResponse completeRegistration(RegisterCompleteRequest request) {
        String registrationToken = request.getRegistrationToken();
        String identifier = registrationTokenService.requireIdentifier(registrationToken);
        
        // Identifier is always mobile now
        String mobile = identifier;
        
        // Consume the token so it cannot be used again
        registrationTokenService.consume(registrationToken);
        
        String passwordHash = passwordService.hashPassword(request.getPassword());
        
        User user = userService.createNewUser(
                request.getFirstName(),
                request.getLastName(),
                passwordHash,
                mobile
        );

        return tokenIssuanceService.issueTokens(user);
    }


}
