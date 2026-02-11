package az.fitnest.identity.service;

import az.fitnest.identity.dto.RegisterCompleteRequest;
import az.fitnest.identity.dto.RegisterRequest;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.constants.OtpPurpose;
import az.fitnest.identity.entity.OtpVerificationResult;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.exception.ConflictException;
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
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.getMobile());
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
