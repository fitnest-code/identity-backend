package az.fitnest.identity.controller;

import az.fitnest.identity.dto.response.RoleResponse;
import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Rol İdarəetmə Admin", description = "Sistem rollarının idarə olunması üçün admin endpointləri.")
public class RoleAdminController {
    private final RoleRepository roleRepository;
    private final UserServiceImpl userService;

    @Operation(summary = "Yeni rol yaradın", description = "Yeni sistem rolu yaradılır.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Role> createRole(@RequestParam String name) {
        if (roleRepository.findByName(name).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        Role role = new Role();
        role.setName(name);
        Role saved = roleRepository.save(role);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Bütün rolları əldə edin", description = "Sistemdəki bütün rolları qaytarır.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/raw")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @Operation(summary = "Rol ID ilə əldə edin", description = "Rol ID-si ilə rolu qaytarır.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{roleId}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long roleId) {
        return roleRepository.findById(roleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Rol adını yeniləyin", description = "Rol adını yeniləyir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{roleId}")
    public ResponseEntity<Role> updateRole(@PathVariable Long roleId, @RequestParam String name) {
        Role role = roleRepository.findById(roleId)
                .orElse(null);
        if (role == null) return ResponseEntity.notFound().build();
        if (roleRepository.findByName(name).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        role.setName(name);
        Role updated = roleRepository.save(role);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Rol silin", description = "Rolu silir.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        userService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mövcud rolları əldə edin", description = "Bütün sistem rollarını ierarxik sırada qaytarır. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAvailableRoles() {
        return ResponseEntity.ok(userService.getAvailableRoles());
    }
}
