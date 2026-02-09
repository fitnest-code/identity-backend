package az.fitnest.iam.setup.api.dto.request;

import az.fitnest.iam.setup.api.validation.ValidGender;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotNull(message = "height_cm is required")
    @Min(value = 50, message = "height_cm must be between 50 and 250")
    @Max(value = 250, message = "height_cm must be between 50 and 250")
    @JsonProperty("height_cm")
    private Integer heightCm;

    @NotNull(message = "weight_kg is required")
    @Min(value = 10, message = "weight_kg must be between 10 and 500")
    @Max(value = 500, message = "weight_kg must be between 10 and 500")
    @JsonProperty("weight_kg")
    private Double weightKg;

    @NotNull(message = "gender is required")
    @ValidGender
    @JsonProperty("gender")
    private String gender;

    @NotNull(message = "age is required")
    @Min(value = 10, message = "age must be between 10 and 999")
    @Max(value = 999, message = "age must be between 10 and 999")
    @JsonProperty("age")
    private Integer age;
}
