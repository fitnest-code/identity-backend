package az.fitnest.identity.controller;

import az.fitnest.identity.grpc.CreateGymAdminRequest;
import az.fitnest.identity.grpc.CreateGymAdminResponse;
import az.fitnest.identity.grpc.IdentityServiceGrpc;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.repository.RoleRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.grpc.Status;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class IdentityServiceGrpcImpl extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createGymAdmin(CreateGymAdminRequest request, StreamObserver<CreateGymAdminResponse> responseObserver) {
        log.info("gRPC: Creating gym admin for email: {}", request.getEmail());
        try {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            
            // 1. Ensure the role exists
            String roleName = "ROLE_GYM_SUPER_ADMIN";
            roleRepository.findByName(roleName).orElseGet(() -> {
                log.info("Role {} not found, creating it...", roleName);
                Role newRole = new Role();
                newRole.setName(roleName);
                return roleRepository.save(newRole);
            });

            // 2. Create the user
            User user = userService.createNewUser(request.getName(), request.getSurname(), encodedPassword, request.getPhoneNumber());
            
            // 3. Update profile with email (this now correctly passes email to user-backend)
            az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest profileReq = 
                new az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest(request.getName(), request.getSurname(), request.getEmail(), request.getPhoneNumber());
            userService.updateUserProfile(user.getId(), profileReq);
            
            // 4. Assign role
            userService.updateUserRole(user.getId(), "GYM_SUPER_ADMIN");
            
            log.info("Successfully created gym admin with ID: {}", user.getId());
            
            CreateGymAdminResponse response = CreateGymAdminResponse.newBuilder()
                    .setUserId(user.getId())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to create gym admin: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create gym admin: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
