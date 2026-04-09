package az.fitnest.identity.configuration;


import az.fitnest.identity.model.enums.LegalDocumentType;
import az.fitnest.identity.model.entity.*;
import az.fitnest.identity.repository.*;
import az.fitnest.identity.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final LegalDocumentRepository legalDocumentRepository;
    private final UserConsentRepository userConsentRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            initRoles();
            initAdminUser();
            initSuperAdminUser();
            initRegularUser();
            initRegularUser2();
            initPartnerUser();
            initPartnerUser2();
            initLegalDocuments();
            initUserConsents();
        };
    }

    private void initRoles() {
        createRoleIfNotFound("ROLE_USER");
        createRoleIfNotFound("ROLE_ADMIN");
        createRoleIfNotFound("ROLE_SUPER_ADMIN");
        createRoleIfNotFound("ROLE_PARTNER");
    }

    private void createRoleIfNotFound(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    private void initAdminUser() {
        String adminMobile = az.fitnest.identity.util.MobileNumberUtils.normalize("0500000000");
        Optional<User> adminOptional = userRepository.findFirstByMobile(adminMobile);

        if (adminOptional.isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role newAdminRole = new Role();
                        newAdminRole.setName("ROLE_ADMIN");
                        return roleRepository.save(newAdminRole);
                    });

            User admin = new User();
            admin.setMobile(adminMobile);
            admin.setPasswordHash(passwordService.hashPassword("Admin123!"));
            admin.setHasAccount(true);
            admin.setSetupRequired(false);
            admin.setRole(adminRole);

            userRepository.save(admin);
        }
    }

    private void initSuperAdminUser() {
        String superAdminMobile = az.fitnest.identity.util.MobileNumberUtils.normalize("0510000000");
        Optional<User> superAdminOptional = userRepository.findFirstByMobile(superAdminMobile);

        if (superAdminOptional.isEmpty()) {
            Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName("ROLE_SUPER_ADMIN");
                        return roleRepository.save(newRole);
                    });

            User superAdmin = new User();
            superAdmin.setMobile(superAdminMobile);
            superAdmin.setPasswordHash(passwordService.hashPassword("SuperAdmin123!"));
            superAdmin.setHasAccount(true);
            superAdmin.setSetupRequired(false);
            superAdmin.setRole(superAdminRole);

            userRepository.save(superAdmin);
        }
    }

    private void initRegularUser() {
        String userMobile = az.fitnest.identity.util.MobileNumberUtils.normalize("0550000000");
        if (userRepository.findFirstByMobile(userMobile).isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);
            User user = new User();
            user.setMobile(userMobile);
            user.setPasswordHash(passwordService.hashPassword("User123!"));
            user.setHasAccount(true);
            user.setSetupRequired(false);
            user.setRole(userRole);
            userRepository.save(user);
        }
    }

    private void initPartnerUser() {
        String partnerMobile = az.fitnest.identity.util.MobileNumberUtils.normalize("0700000000");
        if (userRepository.findFirstByMobile(partnerMobile).isEmpty()) {
            Role partnerRole = roleRepository.findByName("ROLE_PARTNER").orElse(null);
            User user = new User();
            user.setMobile(partnerMobile);
            user.setPasswordHash(passwordService.hashPassword("Partner123!"));
            user.setHasAccount(true);
            user.setSetupRequired(false);
            user.setRole(partnerRole);
            userRepository.save(user);
        }
    }

    private void initRegularUser2() {
        String userMobile = az.fitnest.identity.util.MobileNumberUtils.normalize("0550000001");
        if (userRepository.findFirstByMobile(userMobile).isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);
            User user = new User();
            user.setMobile(userMobile);
            user.setPasswordHash(passwordService.hashPassword("User123!"));
            user.setHasAccount(true);
            user.setSetupRequired(false);
            user.setRole(userRole);
            userRepository.save(user);
        }
    }

    private void initPartnerUser2() {
        String partnerMobile = az.fitnest.identity.util.MobileNumberUtils.normalize("0700000001");
        if (userRepository.findFirstByMobile(partnerMobile).isEmpty()) {
            Role partnerRole = roleRepository.findByName("ROLE_PARTNER").orElse(null);
            User user = new User();
            user.setMobile(partnerMobile);
            user.setPasswordHash(passwordService.hashPassword("Partner123!"));
            user.setHasAccount(true);
            user.setSetupRequired(false);
            user.setRole(partnerRole);
            userRepository.save(user);
        }
    }

    private void initLegalDocuments() {
        if (legalDocumentRepository.count() == 0) {
            legalDocumentRepository.save(LegalDocument.builder()
                    .type(LegalDocumentType.PRIVACY_POLICY)
                    .version("1.0")
                    .language("EN")
                    .content("<h1>Privacy Policy</h1><p>Your privacy is important to us...</p>")
                    .isActive(true)
                    .publishedAt(LocalDateTime.now())
                    .build());

            legalDocumentRepository.save(LegalDocument.builder()
                    .type(LegalDocumentType.TERMS_OF_USE)
                    .version("1.0")
                    .language("EN")
                    .content("<h1>Terms of Use</h1><p>By using this app, you agree to...</p>")
                    .isActive(true)
                    .publishedAt(LocalDateTime.now())
                    .build());

            legalDocumentRepository.save(LegalDocument.builder()
                    .type(LegalDocumentType.PRIVACY_POLICY)
                    .version("1.0")
                    .language("AZ")
                    .content("<h1>Məxfilik Siyasəti</h1><p>Sizin məxfiliyiniz bizim üçün vacibdir...</p>")
                    .isActive(true)
                    .publishedAt(LocalDateTime.now())
                    .build());
        }
    }

    private void initUserConsents() {
        if (userConsentRepository.count() == 0) {
            userRepository.findAll().forEach(user -> {
                userConsentRepository.save(UserConsent.builder()
                        .userId(user.getId())
                        .privacyPolicyVersion("1.0")
                        .termsOfUseVersion("1.0")
                        .acceptedAt(LocalDateTime.now())
                        .platform("WEB")
                        .build());
            });
        }
    }
}
