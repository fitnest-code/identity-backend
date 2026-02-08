package az.fitnest.iam.auth.adapter.persistence;

import az.fitnest.iam.auth.domain.enums.SocialProvider;
import az.fitnest.iam.auth.domain.model.SocialAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {

    Optional<SocialAuth> findByProviderAndProviderId(
            SocialProvider provider,
            String providerId
    );



    boolean existsByProviderAndProviderId(
            SocialProvider provider,
            String providerId
    );
}