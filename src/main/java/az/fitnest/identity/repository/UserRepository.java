package az.fitnest.identity.repository;

import az.fitnest.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users WHERE mobile = :mobile AND status = 'ACTIVE' LIMIT 1", nativeQuery = true)
    Optional<User> findByMobile(@Param("mobile") String mobile);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE mobile = :mobile AND status = 'ACTIVE'", nativeQuery = true)
    boolean existsByMobile(@Param("mobile") String mobile);

    /**
     * Native query that returns users regardless of their status (ACTIVE/INACTIVE)
     * so we can distinguish between "non-existent" and "inactive" users.
     */
    @Query(value = "SELECT * FROM users WHERE mobile = :mobile LIMIT 1", nativeQuery = true)
    Optional<User> findByMobileIncludingDeleted(@Param("mobile") String mobile);

    @Query(value = "SELECT * FROM users WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE mobile = :mobile", nativeQuery = true)
    boolean existsByMobileIncludingDeleted(@Param("mobile") String mobile);
}