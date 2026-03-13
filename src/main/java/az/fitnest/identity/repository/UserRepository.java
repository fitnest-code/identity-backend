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

    /**
     * Batch-friendly version: paginates inactive user IDs for deletion.
     */
    @Query("""
        SELECT u.id FROM User u
        WHERE u.status = 'INACTIVE' AND u.inactiveAt < :threshold
    """)
    Page<Long> findInactiveUserIds(@Param("threshold") Instant threshold, Pageable pageable);

    /**
     * Batch deletion for large datasets. Deletes inactive users in batches of batchSize.
     * Avoids memory and locking issues.
     */
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

    @Query("""
        SELECT u FROM User u
        WHERE (:id IS NULL OR u.id = :id)
          AND (:name IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:surname IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :surname, '%')))
          AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
          AND (:mobile IS NULL OR u.mobile LIKE CONCAT('%', :mobile, '%'))
    """)
    Page<User> searchUsersAdvanced(@Param("id") Long id,
                              @Param("name") String name,
                              @Param("surname") String surname,
                              @Param("email") String email,
                              @Param("mobile") String mobile,
                              Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE (:id IS NULL OR u.id = :id)
          AND (:name IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:surname IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :surname, '%')))
          AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
          AND (:mobile IS NULL OR u.mobile LIKE CONCAT('%', :mobile, '%'))
    """)
    Page<User> searchUsers(@Param("id") Long id,
                      @Param("name") String name,
                      @Param("surname") String surname,
                      @Param("email") String email,
                      @Param("mobile") String mobile,
                      Pageable pageable);

    Page<User> findByIdIn(List<Long> userIds, Pageable pageable);

    // --- Performance Recommendations ---
    // For searchUsersAdvanced/searchUsers: Add DB indexes on firstName, lastName, email, mobile, status, inactiveAt.
    // For frequent search combinations, use composite indexes.
    // For LIKE queries on large tables, consider full-text search or native SQL.
    // For markNoSessionsIfNone: Ensure AuthToken.userId is indexed.
    // For existsByRole: Consider caching if called frequently.
    // For very large selects: Use Spring Data Stream<User> or Scroll for batch processing.
    // Example index DDL (PostgreSQL):
    // CREATE INDEX idx_user_firstname ON user (first_name);
    // CREATE INDEX idx_user_lastname ON user (last_name);
    // CREATE INDEX idx_user_email ON user (email);
    // CREATE INDEX idx_user_mobile ON user (mobile);
    // CREATE INDEX idx_user_status_inactiveat ON user (status, inactive_at);
    // CREATE INDEX idx_authtoken_userid ON authtoken (user_id);
}
