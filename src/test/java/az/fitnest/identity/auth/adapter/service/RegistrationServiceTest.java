package az.fitnest.identity.auth.adapter.service;

import az.fitnest.identity.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.identity.auth.api.dto.request.RegisterRequest;
import az.fitnest.identity.auth.api.dto.response.LoginResponse;
import az.fitnest.identity.otp.adapter.service.OtpService;
import az.fitnest.identity.otp.api.dto.request.OtpSendRequest;
import az.fitnest.identity.user.adapter.persistence.UserRepository;
import az.fitnest.identity.user.adapter.service.UserService;
import az.fitnest.identity.user.domain.model.User;
import az.fitnest.identity.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void startRegistration_shouldCallOtpService_whenMobileProvided() {
        RegisterRequest request = new RegisterRequest();
        request.setMobile("0501234567");
        
        when(userRepository.findByMobileIncludingDeleted("+994501234567")).thenReturn(Optional.empty());
        
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
        RegisterRequest request = new RegisterRequest();
        request.setMobile("0501234567");
        
        when(userRepository.findByMobileIncludingDeleted("+994501234567")).thenReturn(Optional.of(new User()));
        
        assertThrows(ConflictException.class, () -> registrationService.startRegistration(request));
    }
    
    @Test
    void completeRegistration_shouldCreateUser_whenTokenValid() {
        RegisterCompleteRequest request = new RegisterCompleteRequest();
        request.setRegistrationToken("valid-token");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("password");
        
        when(registrationTokenService.requireIdentifier("valid-token")).thenReturn("+994501234567");
        when(passwordService.hashPassword("password")).thenReturn("hashedPass");
        when(userService.createNewUser("John", "Doe", "hashedPass", "+994501234567")).thenReturn(new User());
        when(tokenIssuanceService.issueTokens(any())).thenReturn(new LoginResponse());

        registrationService.completeRegistration(request);
        
        verify(registrationTokenService).consume("valid-token");
        verify(userService).createNewUser("John", "Doe", "hashedPass", "+994501234567");
    }
}
