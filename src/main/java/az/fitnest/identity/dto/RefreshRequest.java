package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "refreshToken")
public class RefreshRequest {

    @NotBlank
    @JsonAlias("refresh_token")
    private String refreshToken;
}