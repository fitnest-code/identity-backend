package az.fitnest.identity.repository;

import az.fitnest.identity.constants.RoleName;
import az.fitnest.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
