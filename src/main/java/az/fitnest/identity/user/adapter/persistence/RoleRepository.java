package az.fitnest.identity.user.adapter.persistence;

import az.fitnest.identity.user.domain.enums.RoleName;
import az.fitnest.identity.user.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
