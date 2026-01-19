package az.fitnest.iamservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OTPSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_session_id")
    private Long otpSessionId;
    
    @Column(name = "mobile", nullable = false)
    private String mobile;
    
    @Column(name = "otp_code", nullable = false)
    private String otpCode;
    
    @Column(name = "purpose")
    private String purpose;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "attempts")
    private Integer attempts;
    
    @Column(name = "status")
    private String status;
}
