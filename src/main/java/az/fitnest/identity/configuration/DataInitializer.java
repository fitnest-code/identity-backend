package az.fitnest.identity.configuration;

import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.constants.RoleName;
import az.fitnest.identity.entity.Role;
import az.fitnest.identity.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordService passwordService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            initRoles();
            initAdminUser();
        };
    }

    private void initRoles() {
        createRoleIfNotFound(RoleName.ROLE_USER);
        createRoleIfNotFound(RoleName.ROLE_ADMIN);
    }

    private void createRoleIfNotFound(RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    private void initAdminUser() {
        String adminMobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize("0500000000");
        Optional<User> adminOptional = userRepository.findByMobileIncludingDeleted(adminMobile);

        if (adminOptional.isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseGet(() -> {
                        Role newAdminRole = new Role();
                        newAdminRole.setName(RoleName.ROLE_ADMIN);
                        return roleRepository.save(newAdminRole);
                    });
            
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setMobile(adminMobile);
            admin.setPasswordHash(passwordService.hashPassword("Admin123!"));
            admin.setHasAccount(true);
            admin.setSetupRequired(false);
            admin.setRole(adminRole);

            userRepository.save(admin);
        }
    }
}
