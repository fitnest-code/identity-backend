package az.fitnest.identity.config;

import az.fitnest.identity.auth.adapter.service.PasswordService;
import az.fitnest.identity.user.adapter.persistence.RoleRepository;
import az.fitnest.identity.user.adapter.persistence.UserRepository;
import az.fitnest.identity.user.domain.enums.RoleName;
import az.fitnest.identity.user.domain.model.Role;
import az.fitnest.identity.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            initRoles();
            initAdminUser();
        };
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            log.info("Initializing roles...");
            roleRepository.save(new Role(null, RoleName.ROLE_USER));
            roleRepository.save(new Role(null, RoleName.ROLE_ADMIN));
        }
    }

    private void initAdminUser() {
        String adminMobile = "0500000000"; // Example admin mobile
        Optional<User> adminOptional = userRepository.findByMobileIncludingDeleted(adminMobile);

        if (adminOptional.isEmpty()) {
            log.info("Creating default admin user...");
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .mobile(adminMobile)
                    .passwordHash(passwordService.hashPassword("Admin123!"))
                    .hasAccount(true)
                    .setupRequired(false)
                    .role(adminRole)
                    .build();
            
            userRepository.save(admin);
        }
    }
}
