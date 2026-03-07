package az.fitnest.identity.repository;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    long deleteByUserId(Long userId);

    List<AuthToken> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long deleteByAccessTokenHash(String accessTokenHash);

    AuthToken findByRefreshTokenHash(String refreshTokenHash);

    AuthToken findByJti(String jti);

    long deleteByRefreshTokenHash(String refreshTokenHash);

    long deleteByJti(String jti);

    @Modifying
    @Transactional
    @Query("""
                DELETE FROM AuthToken t
                WHERE t.userId = :userId
                  AND t.refreshTokenHash = :hash
                  AND t.revoked = false
                  AND (t.refreshExpiresAt IS NULL OR t.refreshExpiresAt > :now)
            """)
    int consumeRefreshToken(@Param("userId") Long userId, @Param("hash") String hash, @Param("now") Instant now);
}
