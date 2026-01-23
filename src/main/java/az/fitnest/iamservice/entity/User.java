package az.fitnest.iamservice.entity;

import az.fitnest.iamservice.enums.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "surname", nullable = false)
    private String surname;
    
    @Column(name = "mobile", unique = true)
    private String mobile;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private Language language;
    
    @Column(name = "setup_required")
    private Boolean setupRequired;
    
    @Column(name = "has_account")
    private Boolean hasAccount;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "account_locked")
    private Boolean accountLocked;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
