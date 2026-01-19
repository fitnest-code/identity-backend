package az.fitnest.iamservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationToken {
    
    @Id
    @Column(name = "token", nullable = false)
    private String token;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "mobile")
    private String mobile;
    
    @Column(name = "user_reference")
    private String userReference;
}
