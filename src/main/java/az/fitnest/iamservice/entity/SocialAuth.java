package az.fitnest.iamservice.entity;

import az.fitnest.iamservice.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "social_auths")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocialAuth {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_auth_id")
    private Long socialAuthId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialProvider provider;
    
    @Column(name = "provider_id", nullable = false)
    private String providerId;
    
    @Column(name = "email")
    private String email;
}
