package az.fitnest.iamservice.entity;

import az.fitnest.iamservice.enums.OtpPurpose;
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
    
    @Column(name = "session_id", unique = true)
    private String sessionId;
    
    @Column(name = "mobile", nullable = false)
    private String mobile;
    
    @Column(name = "otp_code", nullable = false)
    private String otpCode;
    
    @Column(name = "purpose")
    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "attempts")
    private Integer attempts;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "verified")
    private Boolean verified;
    
    @Column(name = "locked")
    private Boolean locked;
    
    @Column(name = "resend_available_at")
    private LocalDateTime resendAvailableAt;
}
