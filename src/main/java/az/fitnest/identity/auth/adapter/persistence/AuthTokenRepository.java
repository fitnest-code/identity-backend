package az.fitnest.identity.auth.adapter.persistence;

import az.fitnest.identity.auth.domain.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    long deleteByUserId(Long userId);
    
    List<AuthToken> findByUserId(Long userId);
}