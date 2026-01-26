package az.fitnest.iam.auth.adapter.persistence;

import az.fitnest.iam.auth.domain.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    long deleteByUserId(Long userId);
}