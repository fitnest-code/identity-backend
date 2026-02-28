package az.fitnest.identity.repository;

import az.fitnest.identity.entity.SessionStatus;
import az.fitnest.identity.entity.User;
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

    Optional<User> findFirstByMobileAndStatus(String mobile, User.Status status);

    boolean existsByMobileAndStatus(String mobile, User.Status status);

    Optional<User> findFirstByMobile(String mobile);

    boolean existsByMobile(String mobile);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    int incrementFailedLoginAttempts(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts, u.lockedUntil = :lockedUntil, u.status = :status WHERE u.id = :userId")
    int updateLockStatus(@Param("userId") Long userId, @Param("attempts") int attempts, @Param("lockedUntil") Instant lockedUntil, @Param("status") User.Status status);

    @Modifying
    @Transactional
    @Query("""
        UPDATE User u
        SET u.sessionStatus = :status
        WHERE u.id = :userId
          AND NOT EXISTS (SELECT 1 FROM AuthToken t WHERE t.userId = :userId)
    """)
    int markNoSessionsIfNone(@Param("userId") Long userId, @Param("status") SessionStatus status);

    /**
     * Finds a user by mobile number, including deleted users.
     * This is used for registration and conflict checks.
     */
    @Query("SELECT u FROM User u WHERE u.mobile = :mobile")
    Optional<User> findByMobileIncludingDeleted(@Param("mobile") String mobile);
}