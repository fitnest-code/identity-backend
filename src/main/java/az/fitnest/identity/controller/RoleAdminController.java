package az.fitnest.identity.controller;

import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.service.impl.UserServiceImpl;
import az.fitnest.identity.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {
    private final RoleRepository roleRepository;
    private final UserServiceImpl userService;

    @Operation(summary = "Create a new role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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

    @Operation(summary = "Get all roles")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @Operation(summary = "Get role by ID")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{roleId}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long roleId) {
        return roleRepository.findById(roleId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update role name")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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

    @Operation(summary = "Delete role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        userService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }
}
