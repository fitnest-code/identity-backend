package az.fitnest.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin üçün istifadəçi məlumatları")
public record AdminUserResponse(
    @Schema(description = "İstifadəçi ID-si", example = "123")
    Long id,

    @Schema(description = "Ad", example = "Kamal")
    String name,

    @Schema(description = "Soyad", example = "Əliyev")
    String surname,

    @Schema(description = "Mobil nömrə", example = "0501234567")
    String phoneNumber,

    @Schema(description = "Email", example = "kamal@fitnest.az")
    String email,

    @Schema(description = "Status (ACTIVE, INACTIVE, LOCKED, NO_SESSIONS)", example = "ACTIVE")
    String status,

    @Schema(description = "Abunə statusu (aktiv, dondurulmuş və s.)", example = "aktiv")
    String subscriptionStatus
) {}
