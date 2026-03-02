package az.fitnest.identity.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventId;
    private Long timestamp;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, String> variables;

    public enum NotificationType {
        EMAIL, SMS, PUSH
    }
}
