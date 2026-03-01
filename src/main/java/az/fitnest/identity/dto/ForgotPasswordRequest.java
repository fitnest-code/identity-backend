package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Mobil nömrə tələb olunur")
    @jakarta.validation.constraints.Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Yanlış mobil nömrə formatı. 050, 051, 010, 055, 099, 070, 077 və ya 060 ilə başlamalı və 7 rəqəmlə davam etməlidir.")
    private String mobile;
}
