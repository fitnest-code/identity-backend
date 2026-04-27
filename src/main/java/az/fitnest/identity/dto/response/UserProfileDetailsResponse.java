package az.fitnest.identity.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserProfileDetailsResponse(
        Long id,
        String registrationDate,
        String platform,
        String phoneNumber,
        String email,
        LocalDate birthDate,
        String goal,
        Double height,
        Double weight,
        Double bmiIndex
) {
}
