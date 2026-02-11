package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSetupCompletedEvent {
    private String eventId;
    private Long userId;
    private Long timestamp;
    private String source; // identity-service
}
