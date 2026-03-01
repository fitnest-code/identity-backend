package az.fitnest.identity.repository;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    Optional<UserConsent> findTopByUserIdOrderByAcceptedAtDesc(Long userId);

    List<UserConsent> findAllByUserId(Long userId);

    Page<UserConsent> findAllByUserIdOrderByAcceptedAtDesc(Long userId, Pageable pageable);

    Page<UserConsent> findAllByOrderByAcceptedAtDesc(Pageable pageable);
}
