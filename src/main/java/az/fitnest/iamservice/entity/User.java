package az.fitnest.iamservice.entity;

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
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(name = "mobile", unique = true)
    private String mobile;
    
    @Column(name = "email", unique = true)
    private String email;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "language")
    private String language;
    
    @Column(name = "setup_required")
    private Boolean setupRequired;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
