package az.fitnest.identity.repository;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SessionStatus;
import az.fitnest.identity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findFirstByMobileAndStatus(String mobile, UserStatus status);

    boolean existsByMobileAndStatus(String mobile, UserStatus status);

    Optional<User> findFirstByMobile(String mobile);

    boolean existsByMobile(String mobile);

    Optional<User> findFirstByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    int incrementFailedLoginAttempts(@Param("userId") Long userId);

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
        WHERE u.status != 'INACTIVE' AND u.role.name != 'ROLE_SUPER_ADMIN'
    """)
    int deactivateAllNonAdmins(@Param("now") Instant now);

    @Query("""
        SELECT u.id FROM User u
        WHERE u.status = 'INACTIVE' AND u.inactiveAt < :threshold
    """)
    java.util.List<Long> findInactiveUserIds(@Param("threshold") Instant threshold);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM User u WHERE u.id IN :ids
    """)
    int deleteUsersByIds(@Param("ids") java.util.List<Long> ids);
}
