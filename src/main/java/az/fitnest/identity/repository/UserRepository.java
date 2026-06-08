package az.fitnest.identity.repository;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SessionStatus;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findFirstByMobileAndStatus(String mobile, UserStatus status);

    boolean existsByMobileAndStatus(String mobile, UserStatus status);

    Optional<User> findFirstByMobile(String mobile);

    boolean existsByMobile(String mobile);

    @Query(value = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1 WHERE user_id = :userId RETURNING failed_login_attempts", nativeQuery = true)
    Integer incrementFailedLoginAttemptsAndReturn(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.lockedUntil = :lockedUntil, u.status = :status WHERE u.id = :userId")
    int updateLockStatus(@Param("userId") Long userId, @Param("attempts") int attempts, @Param("lockedUntil") Instant lockedUntil, @Param("status") UserStatus status);

    @Modifying
    @Transactional
    @Query("""
                UPDATE User u
                SET u.sessionStatus = :status
                WHERE u.id = :userId
                  AND NOT EXISTS (SELECT 1 FROM AuthToken t WHERE t.userId = :userId)
            """)
    int markNoSessionsIfNone(@Param("userId") Long userId, @Param("status") SessionStatus status);

    @Query("SELECT u FROM User u WHERE u.mobile = :mobile")
    Optional<User> findByMobileIncludingDeleted(@Param("mobile") String mobile);

    @Modifying
    @Transactional
    @Query("""
                UPDATE User u SET u.status = 'INACTIVE', u.inactiveAt = :now
                WHERE u.status != 'INACTIVE' AND u.role.name != 'ROLE_ADMIN'
            """)
    int deactivateAllNonAdmins(@Param("now") Instant now);

    @Query("""
                SELECT u.id FROM User u
                WHERE u.status = 'INACTIVE' AND u.inactiveAt < :threshold
            """)
    Page<Long> findInactiveUserIds(@Param("threshold") Instant threshold, Pageable pageable);

    @Modifying
    @Transactional
    default void deleteInactiveUsersBeforeBatch(Instant threshold, int batchSize) {
        Page<Long> page;
        do {
            page = findInactiveUserIds(threshold, org.springframework.data.domain.PageRequest.of(0, batchSize));
            List<Long> ids = page.getContent();
            if (!ids.isEmpty()) {
                deleteUsersByIds(ids);
            }
        } while (!page.isEmpty());
    }

    @Modifying
    @Transactional
    @Query("""
                DELETE FROM User u WHERE u.id IN :ids
            """)
    int deleteUsersByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.status = 'INACTIVE' AND u.inactiveAt < :threshold")
    void deleteInactiveUsersBefore(@Param("threshold") Instant threshold);

    boolean existsByRole(Role role);

    @Query(value = """
            SELECT u.* FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE (:id IS NULL OR u.user_id = :id)
              AND (:mobile IS NULL OR u.mobile LIKE :mobile || '%')
              AND (:userIds IS NULL OR u.user_id IN :userIds)
              AND (:roleName IS NULL OR r.name = :roleName)
        """, nativeQuery = true)
    Page<User> searchUsersAdvanced(@Param("id") Long id,
                                   @Param("name") String name,
                                   @Param("surname") String surname,
                                   @Param("email") String email,
                                   @Param("mobile") String mobile,
                                   @Param("userIds") List<Long> userIds,
                                   @Param("roleName") String roleName,
                                   Pageable pageable);

    @Query(value = """
                SELECT * FROM users u1_0
                WHERE (:id IS NULL OR u1_0.user_id = :id)
                  AND (:mobile IS NULL OR u1_0.mobile LIKE :mobile || '%')
            """, nativeQuery = true)
    Page<User> searchUsers(@Param("id") Long id,
                           @Param("name") String name,
                           @Param("surname") String surname,
                           @Param("email") String email,
                           @Param("mobile") String mobile,
                           Pageable pageable);

    Page<User> findByIdIn(List<Long> userIds, Pageable pageable);

    @Query("SELECT u.id FROM User u WHERE u.mobile LIKE CONCAT('%', :query, '%')")
    List<Long> findUserIdsByMobileContaining(@Param("query") String query);

    @Query("SELECT u.id FROM User u WHERE u.role.name IN :roleNames")
    List<Long> findUserIdsByRoleNames(@Param("roleNames") List<String> roleNames);

    @Query("""
        SELECT u.id FROM User u
        WHERE u.role.name IN :roleNames
           OR (u.role.name IN :partnerRoles AND u.deviceId IS NOT NULL AND u.deviceId != '')
    """)
    List<Long> findUserIdsByRoleNamesOrPartnersWithMobile(@Param("roleNames") List<String> roleNames, @Param("partnerRoles") List<String> partnerRoles);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.language = :language WHERE u.id = :userId")
    int updateLanguage(@Param("userId") Long userId, @Param("language") String language);

}
