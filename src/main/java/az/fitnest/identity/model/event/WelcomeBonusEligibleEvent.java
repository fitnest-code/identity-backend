package az.fitnest.identity.model.event;

/**
 * Local Spring event; Kafka publish happens after the surrounding transaction commits.
 */
public record WelcomeBonusEligibleEvent(Long userId, String phone, String email) {
}
