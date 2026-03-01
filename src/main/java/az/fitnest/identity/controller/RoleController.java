package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Rolları idarə etmək üçün ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @Operation(summary = "Bütün rolları əldə edin", description = "Bütün rolların siyahısını qaytarır. SUPER_ADMIN rolu tələb olunur.")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @PostMapping
    @Operation(summary = "Rol yaradın", description = "Yeni rol yaradır. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rol uğurla yaradıldı"),
            @ApiResponse(responseCode = "400", description = "Yanlış rol adı")
    })
    public ResponseEntity<Role> createRole(@RequestBody String roleName) {
        if (roleRepository.findByName(roleName).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        Role role = new Role();
        role.setName(roleName);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleRepository.save(role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rolu silin", description = "ID vasitəsilə rolu silir. SUPER_ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rol uğurla silindi"),
            @ApiResponse(responseCode = "404", description = "Rol tapılmadı")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
