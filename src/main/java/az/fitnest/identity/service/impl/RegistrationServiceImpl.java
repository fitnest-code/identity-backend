package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.response.OtpSendResponse;
import az.fitnest.identity.dto.request.RegisterCompleteRequest;
import az.fitnest.identity.dto.request.RegisterRequest;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.service.WelcomeBonusService;
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
    private final LegalService legalService;
    private final WelcomeBonusService welcomeBonusService;

    @Override
    public OtpSendResponse startRegistration(RegisterRequest request) {
        String normalizedMobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.mobile());

        if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
            throw new ConflictException("error.registration.duplicate_mobile", "DUPLICATE_MOBILE");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.REGISTRATION, normalizedMobile, null, null);

        return otpService.sendOtp(
                otpRequest,
                null,
                null,
                null,
                normalizedMobile
        );
    }

    @Transactional
    @Override
    public LoginResponse completeRegistration(RegisterCompleteRequest request) {
        String registrationToken = request.registrationToken();
        String identifier = registrationTokenService.requireIdentifier(registrationToken);
        String mobile = identifier;
        registrationTokenService.consume(registrationToken);
        String passwordHash = passwordService.hashPassword(request.password());
        User user = userService.createNewUser(
                request.firstName(),
                request.lastName(),
                passwordHash,
                mobile
        );

        legalService.autoAcceptLatestConsents(user.getId());

        publishWelcomeBonusEligible(user);
        return tokenIssuanceService.issueTokens(user, "Web", false);
    }

    @Transactional
    @Override
    public LoginResponse completeRegistrationV2(az.fitnest.identity.dto.request.RegisterCompleteRequestV2 request) {
        String registrationToken = request.registrationToken();
        String identifier = registrationTokenService.requireIdentifier(registrationToken);
        String mobile = identifier;
        registrationTokenService.consume(registrationToken);
        String passwordHash = passwordService.hashPassword(request.password());
        User user = userService.createNewUser(
                request.firstName(),
                request.lastName(),
                passwordHash,
                mobile
        );

        if (request.deviceId() != null && !request.deviceId().isBlank()) {
            user.setDeviceId(request.deviceId());
            user = userRepository.save(user);
        }

        legalService.autoAcceptLatestConsents(user.getId());

        publishWelcomeBonusEligible(user);
        return tokenIssuanceService.issueTokens(user, request.deviceType(), false);
    }

    @Override
    public OtpSendResponse startRegistrationV3(az.fitnest.identity.dto.request.RegisterRequestV3 request) {
        String normalizedMobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.mobile());

        if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
            throw new ConflictException("error.registration.duplicate_mobile", "DUPLICATE_MOBILE");
        }

        OtpSendRequest otpRequest = new OtpSendRequest(OtpPurpose.REGISTRATION, normalizedMobile, null, null);

        return otpService.sendOtp(
                otpRequest,
                null,
                null,
                null,
                normalizedMobile
        );
    }

    @Transactional
    @Override
    public LoginResponse completeRegistrationV3(az.fitnest.identity.dto.request.RegisterCompleteRequestV3 request) {
        String registrationToken = request.registrationToken();
        String identifier = registrationTokenService.requireIdentifier(registrationToken);
        String mobile = identifier;
        registrationTokenService.consume(registrationToken);

        User user = userService.createNewUserV3(
                request.firstName(),
                request.lastName(),
                mobile
        );

        String deviceType = request.deviceType();
        boolean isMobile = "iOS".equalsIgnoreCase(deviceType) || "Android".equalsIgnoreCase(deviceType);

        if (isMobile && request.deviceId() != null && !request.deviceId().isBlank()) {
            user.setDeviceId(request.deviceId());
            user = userRepository.save(user);
        }

        legalService.autoAcceptLatestConsents(user.getId());

        publishWelcomeBonusEligible(user);
        return tokenIssuanceService.issueTokens(user, deviceType, false);
    }

    private void publishWelcomeBonusEligible(User user) {
        welcomeBonusService.tryPublishWelcomeBonusEligible(user);
    }

}
