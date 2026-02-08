package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.iam.auth.api.dto.request.RegisterRequest;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.otp.adapter.service.OtpService;
import az.fitnest.iam.otp.api.dto.request.OtpSendRequest;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.domain.model.User;
import az.fitnest.iam.shared.exception.ConflictException;
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
        request.setMobile("+1234567890");
        
        when(userRepository.findByMobileIncludingDeleted("+1234567890")).thenReturn(Optional.empty());
        
        registrationService.startRegistration(request);
        
        verify(otpService).sendOtp(
            any(OtpSendRequest.class), 
            eq(null), 
            eq(null), 
            eq(null), 
            eq("+1234567890")
        );
    }

    @Test
    void startRegistration_shouldThrowConflict_whenMobileExists() {
        RegisterRequest request = new RegisterRequest();
        request.setMobile("+1234567890");
        
        when(userRepository.findByMobileIncludingDeleted("+1234567890")).thenReturn(Optional.of(new User()));
        
        assertThrows(ConflictException.class, () -> registrationService.startRegistration(request));
    }
    
    @Test
    void completeRegistration_shouldCreateUser_whenTokenValid() {
        RegisterCompleteRequest request = new RegisterCompleteRequest();
        request.setRegistrationToken("valid-token");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("password");
        
        when(registrationTokenService.requireIdentifier("valid-token")).thenReturn("+1234567890");
        when(passwordService.hashPassword("password")).thenReturn("hashedPass");
        when(userService.createNewUser("John", "Doe", "hashedPass", "+1234567890")).thenReturn(new User());
        when(tokenIssuanceService.issueTokens(any())).thenReturn(new LoginResponse());

        registrationService.completeRegistration(request);
        
        verify(registrationTokenService).consume("valid-token");
        verify(userService).createNewUser("John", "Doe", "hashedPass", "+1234567890");
    }
}
