package az.fitnest.iam.user.adapter.persistence;

import az.fitnest.iam.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Native query that ignores @Where(is_deleted = false) filter
     * so we can distinguish between "non-existent" and "deleted" users.
     */
    @Query(value = "SELECT * FROM users WHERE mobile = :mobile LIMIT 1", nativeQuery = true)
    Optional<User> findByMobileIncludingDeleted(@Param("mobile") String mobile);

    @Query(value = "SELECT * FROM users WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM users WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email", nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE mobile = :mobile", nativeQuery = true)
    boolean existsByMobileIncludingDeleted(@Param("mobile") String mobile);
}