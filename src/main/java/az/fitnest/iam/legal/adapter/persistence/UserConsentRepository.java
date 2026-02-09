package az.fitnest.iam.legal.adapter.persistence;

import az.fitnest.iam.legal.domain.model.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    Optional<UserConsent> findTopByUserIdOrderByAcceptedAtDesc(Long userId);
    List<UserConsent> findAllByUserId(Long userId);
}
