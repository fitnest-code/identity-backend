package az.fitnest.iamservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthToken {
    
    @Id
    @Column(name = "access_token", nullable = false)
    private String accessToken;
    
    @Column(name = "refresh_token")
    private String refreshToken;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "revoked")
    private Boolean revoked;
}
