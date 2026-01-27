package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.auth.api.dto.request.RegisterCompleteRequest;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.shared.exception.InvalidCredentialsException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationTokenService registrationTokenService;
    private final UserService userService;
    private final PasswordService passwordService;
    private final UserRepository userRepository;
    private final TokenIssuanceService tokenIssuanceService;

    @Transactional
    public LoginResponse completeRegistration(String registrationToken, RegisterCompleteRequest request) {
        String email = registrationTokenService.requireEmail(registrationToken);
        
        String passwordHash = passwordService.hashPassword(request.getPassword());
        userService.createNewUser(email, request.getFullName(), passwordHash);
        registrationTokenService.consume(registrationToken);
        
        return loginAfterRegistration(email, request.getPassword());
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
