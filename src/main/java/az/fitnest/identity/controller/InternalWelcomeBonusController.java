package az.fitnest.identity.controller;

import az.fitnest.identity.service.WelcomeBonusService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/users/welcome-bonus")
@RequiredArgsConstructor
@Hidden
@Tag(name = "Daxili Welcome Bonus", description = "Welcome bonus status for internal services")
public class InternalWelcomeBonusController {

    private final WelcomeBonusService welcomeBonusService;

    @GetMapping("/pending-ids")
    @Operation(summary = "Bonus almamış istifadəçi ID-ləri")
    public ResponseEntity<Map<String, List<Long>>> findPendingUserIds() {
        return ResponseEntity.ok(Map.of("userIds", welcomeBonusService.findUserIdsPendingWelcomeBonus()));
    }

    @GetMapping("/{userId}/status")
    @Operation(summary = "İstifadəçinin welcome bonus statusu")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "received", welcomeBonusService.isWelcomeBonusReceived(userId)
        ));
    }

    @PutMapping("/{userId}/received")
    @Operation(summary = "Welcome bonus alındı kimi işarələ")
    public ResponseEntity<Void> markReceived(@PathVariable Long userId) {
        welcomeBonusService.markWelcomeBonusReceived(userId);
        return ResponseEntity.noContent().build();
    }
}
