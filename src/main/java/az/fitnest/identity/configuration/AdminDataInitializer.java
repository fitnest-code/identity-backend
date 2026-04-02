package az.fitnest.identity.configuration;

import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.PasswordService;
import az.fitnest.identity.service.UserProfileGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;
    private final UserProfileGrpcClient userProfileGrpcClient;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            return roleRepository.save(role);
        });

        String adminMobile = "+994500000000";
        if (userRepository.findFirstByMobile(adminMobile).isEmpty()) {
            User admin = User.builder()
                    .mobile(adminMobile)
                    .passwordHash(passwordService.hashPassword("Admin@1234!"))
                    .status(UserStatus.ACTIVE)
                    .role(adminRole)
                    .setupRequired(false)
                    .hasAccount(true)
                    .failedLoginAttempts(0)
                    .build();

            User saved = userRepository.save(admin);
            try {
                userProfileGrpcClient.createUserProfile(saved.getId(), "Admin", "Fitnest", "admin@fitnest.az");
                log.info("Admin profile created via gRPC for user ID: {}", saved.getId());
            } catch (Exception e) {
                log.error("Failed to create admin profile via gRPC", e);
            }
            log.info("Admin data initialized successfully with mobile: {} and password: Admin@1234!", adminMobile);
        }
    }
}
