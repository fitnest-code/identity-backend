package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @Email(message = "Email düzgün deyil")
    private String email;

}