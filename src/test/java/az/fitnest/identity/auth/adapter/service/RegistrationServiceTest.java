package az.fitnest.identity.auth.adapter.service;

import az.fitnest.identity.dto.response.LoginResponse;
import az.fitnest.identity.dto.request.OtpSendRequest;
import az.fitnest.identity.dto.request.RegisterCompleteRequest;
import az.fitnest.identity.dto.request.RegisterRequest;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.OtpService;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.RegistrationTokenService;
import az.fitnest.identity.service.TokenIssuanceService;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordService passwordService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenIssuanceService tokenIssuanceService;
    @Mock
    private OtpService otpService;
    @Mock
    private RegistrationTokenService registrationTokenService;

    private RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(
            userService,
            passwordService,
            userRepository,
            tokenIssuanceService,
            otpService,
            registrationTokenService
        );
    }

    @Test
    void startRegistration_shouldCallOtpService_whenMobileProvided() {
        RegisterRequest request = new RegisterRequest("0501234567");

        when(userRepository.findFirstByMobile("+994501234567")).thenReturn(Optional.empty());

        registrationService.startRegistration(request);

        verify(otpService).sendOtp(
            any(OtpSendRequest.class),
            eq(null),
            eq(null),
            eq(null),
            eq("+994501234567")
        );
    }

    @Test
    void startRegistration_shouldThrowConflict_whenMobileExists() {
        RegisterRequest request = new RegisterRequest("0501234567");

        when(userRepository.findFirstByMobile("+994501234567")).thenReturn(Optional.of(new User()));

        assertThrows(ConflictException.class, () -> registrationService.startRegistration(request));
    }

    @Test
    void completeRegistration_shouldCreateUser_whenTokenValid() {
        RegisterCompleteRequest request = new RegisterCompleteRequest("valid-token", "John", "Doe", "password");

        when(registrationTokenService.requireIdentifier("valid-token")).thenReturn("+994501234567");
        when(passwordService.hashPassword("password")).thenReturn("hashedPass");
        when(userService.createNewUser("John", "Doe", "hashedPass", "+994501234567")).thenReturn(new User());
        when(tokenIssuanceService.issueTokens(any(User.class), any())).thenReturn(new LoginResponse("access", "refresh", null));

        registrationService.completeRegistration(request);

        verify(registrationTokenService).consume("valid-token");
        verify(userService).createNewUser("John", "Doe", "hashedPass", "+994501234567");
    }
}
