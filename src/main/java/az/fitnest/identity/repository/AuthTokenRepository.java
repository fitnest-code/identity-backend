package az.fitnest.identity.repository;

import az.fitnest.identity.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    long deleteByUserId(Long userId);
    
    List<AuthToken> findByUserId(Long userId);

    long deleteByAccessToken(String accessToken);
}