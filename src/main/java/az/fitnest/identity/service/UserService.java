package az.fitnest.identity.service;

import az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest;
import az.fitnest.identity.dto.response.RoleResponse;
import az.fitnest.identity.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    User updateUserRole(Long userId, String roleName);

    User getUserById(Long userId);

    Page<User> getAllUsers(Pageable pageable);

    User createNewUser(String firstName, String lastName, String passwordHash, String mobile);

    User createNewUserWithFullName(String fullName, String passwordHash, String mobile);

    User createNewUserV3(String firstName, String lastName, String mobile);

    User updateUserProfile(Long userId, UpdateUserProfileCommandRequest command);

    User updateSetupRequired(Long userId, boolean setupRequired);

    User updateLanguage(Long userId, String language);

    void deactivateUser(Long userId, String reason);

    void deactivateAccount(Long userId, az.fitnest.identity.dto.request.DeactivateAccountRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword);

    void resetUserPasswordDirectly(Long userId, String newPassword);

    Page<az.fitnest.identity.dto.response.UserResponse> getAllUsersMapped(int page, int size);

    void deactivateAllUsers();

    User updateSessionStatus(Long userId, az.fitnest.identity.model.enums.SessionStatus sessionStatus);

    az.fitnest.identity.dto.response.OtpSendResponse requestEmailChange(Long userId, String newEmail);

    User confirmEmailChange(Long userId, String otpSessionId, String otpCode);

    az.fitnest.identity.dto.response.OtpSendResponse requestMobileChange(Long userId, String newMobile);

    User confirmMobileChange(Long userId, String otpSessionId, String otpCode);

    void deleteAccount(Long userId);

    void deleteRole(Long roleId);

    Page<az.fitnest.identity.dto.response.UserResponse> searchUsers(int page, int size, Long id, String name, String surname, String email, String mobile);

    Page<az.fitnest.identity.dto.response.UserResponse> searchUsersAdvanced(int page, int size, String query, Long packageID, Integer durationMonths);

    Page<az.fitnest.identity.dto.response.AdminUserResponse> getAdminUsers(int page, int size, String query, Long packageID, Integer durationMonths, String type, String roles);

    az.fitnest.identity.dto.response.OtpSendResponse resendEmailChangeOtp(Long userId, String otpSessionId);

    az.fitnest.identity.dto.response.OtpSendResponse resendMobileChangeOtp(Long userId, String otpSessionId);

    az.fitnest.identity.dto.response.OtpSendResponse sendOtp(az.fitnest.identity.dto.request.OtpSendRequest request);

    void blockUser(Long userId);

    void unblockUser(Long userId);

    List<RoleResponse> getAvailableRoles();

    void changeUserRole(Long userId, String roleName);

    void hardDeleteUser(Long userId);

    /**
     * Upsert Fitnest staff/admin for cross-environment sync (same mobile + password + role).
     */
    User upsertStaffForEnvSync(String mobile, String rawPassword, String roleName, String firstName, String lastName);
}
