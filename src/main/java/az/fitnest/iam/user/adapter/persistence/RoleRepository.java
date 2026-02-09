package az.fitnest.iam.user.adapter.persistence;

import az.fitnest.iam.user.domain.enums.RoleName;
import az.fitnest.iam.user.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
