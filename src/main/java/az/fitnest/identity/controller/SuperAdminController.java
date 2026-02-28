package az.fitnest.identity.controller;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Super-administrativ tapşırıqlar üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final UserService userService;

    @DeleteMapping("/users/all")
    @Operation(summary = "Bütün istifadəçiləri silin", description = "Bütün qeyri-super-admin istifadəçiləri həmişəlik silir. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bütün istifadəçilər uğurla silindi"),
            @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")
    })
    public ResponseEntity<Void> deleteAllUsers() {
        userService.deleteAllUsers();
        return ResponseEntity.noContent().build();
    }
}
