package az.fitnest.identity.service.impl;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.dto.OtpSendRequest;
import az.fitnest.identity.dto.OtpSendResponse;
import az.fitnest.identity.dto.RegisterCompleteRequest;
import az.fitnest.identity.dto.RegisterRequest;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.util.DeviceDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserService userService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final TokenIssuanceService tokenIssuanceService;
    private final OtpService otpService;
    private final RegistrationTokenService registrationTokenService;

    @Override
    public OtpSendResponse startRegistration(RegisterRequest request) {
        String mobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.getMobile());
        
        if (userRepository.findFirstByMobile(mobile).isPresent()) {
            throw new ConflictException("Bu mobil nömrə artıq qeydiyyatdan keçib");
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
    @Override
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

        return tokenIssuanceService.issueTokens(user, DeviceDetector.detectDeviceType());
    }

}
