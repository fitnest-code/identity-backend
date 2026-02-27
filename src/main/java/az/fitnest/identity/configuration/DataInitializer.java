package az.fitnest.identity.configuration;

import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
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
            initSuperAdminUser();
        };
    }

    private void initRoles() {
        createRoleIfNotFound("ROLE_USER");
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_SUPER_ADMIN");
    }

    private void createRoleIfNotFound(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    private void initAdminUser() {
        String adminMobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize("0500000000");
        Optional<User> adminOptional = userRepository.findFirstByMobile(adminMobile);

        if (adminOptional.isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role newAdminRole = new Role();
                        newAdminRole.setName("ROLE_ADMIN");
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

    private void initSuperAdminUser() {
        String superAdminMobile = az.fitnest.identity.criteria.MobileNumberUtils.normalize("0510000000");
        Optional<User> superAdminOptional = userRepository.findFirstByMobile(superAdminMobile);

        if (superAdminOptional.isEmpty()) {
            Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName("ROLE_SUPER_ADMIN");
                        return roleRepository.save(newRole);
                    });

            User superAdmin = new User();
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setMobile(superAdminMobile);
            superAdmin.setPasswordHash(passwordService.hashPassword("SuperAdmin123!"));
            superAdmin.setHasAccount(true);
            superAdmin.setSetupRequired(false);
            superAdmin.setRole(superAdminRole);

            userRepository.save(superAdmin);
        }
    }
}
