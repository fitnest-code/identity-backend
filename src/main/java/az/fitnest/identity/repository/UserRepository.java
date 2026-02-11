package az.fitnest.identity.repository;

import az.fitnest.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    /**
     * Native query that ignores @Where(is_deleted = false) filter
     * so we can distinguish between "non-existent" and "deleted" users.
     */
    @Query(value = "SELECT * FROM users WHERE mobile = :mobile LIMIT 1", nativeQuery = true)
    Optional<User> findByMobileIncludingDeleted(@Param("mobile") String mobile);

    @Query(value = "SELECT * FROM users WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE mobile = :mobile", nativeQuery = true)
    boolean existsByMobileIncludingDeleted(@Param("mobile") String mobile);
}