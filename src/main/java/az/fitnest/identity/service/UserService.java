package az.fitnest.identity.service;

import az.fitnest.identity.constants.RoleName;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.entity.AuthToken;
import az.fitnest.identity.entity.Role;
import az.fitnest.identity.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.impl.IdentityEventPublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    User updateUserRole(Long userId, RoleName roleName);
    User getUserById(Long userId);
    Page<User> getAllUsers(Pageable pageable);
    User createNewUser(String firstName, String lastName, String passwordHash, String mobile);
    User createNewUserWithFullName(String fullName, String passwordHash, String mobile);
    User updateUserProfile(Long userId, UpdateUserProfileCommand command);
    User updateProfileImageUrl(Long userId, String profileImageUrl);
    User updateSetupRequired(Long userId, boolean setupRequired);
    User updateLanguage(Long userId, String language);
    void deleteUser(Long userId, String reason);
    void deactivateAccount(Long userId);
    void changePassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword);
    Page<az.fitnest.identity.dto.UserResponse> getAllUsersMapped(int page, int size);
}
