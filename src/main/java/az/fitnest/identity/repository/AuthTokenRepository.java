package az.fitnest.identity.repository;

import az.fitnest.identity.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}