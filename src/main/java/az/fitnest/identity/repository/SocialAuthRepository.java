package az.fitnest.identity.repository;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SocialProvider;
import az.fitnest.identity.model.entity.SocialAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    List<SocialAuth> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
