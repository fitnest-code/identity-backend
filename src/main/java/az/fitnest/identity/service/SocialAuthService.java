package az.fitnest.identity.service;

import az.fitnest.identity.constants.SocialProvider;
import az.fitnest.identity.dto.AppleSocialRequest;
import az.fitnest.identity.dto.GoogleSocialRequest;
import az.fitnest.identity.dto.LoginResponse;
import az.fitnest.identity.entity.SocialAuth;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.SocialAuthRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.*;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface SocialAuthService {
    LoginResponse socialLoginApple(AppleSocialRequest request);
    LoginResponse socialLoginGoogle(GoogleSocialRequest request);
}
