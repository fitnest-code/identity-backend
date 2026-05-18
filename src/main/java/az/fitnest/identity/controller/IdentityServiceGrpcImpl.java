package az.fitnest.identity.controller;

import az.fitnest.identity.grpc.CreateGymAdminRequest;
import az.fitnest.identity.grpc.CreateGymAdminResponse;
import az.fitnest.identity.grpc.IdentityServiceGrpc;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.repository.RoleRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.grpc.Status;
import az.fitnest.identity.service.UserProfileGrpcClient;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class IdentityServiceGrpcImpl extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileGrpcClient userProfileGrpcClient;

    @Override
    public void createGymAdmin(CreateGymAdminRequest request, StreamObserver<CreateGymAdminResponse> responseObserver) {
        log.info("gRPC: Creating gym admin for email: {}", request.getEmail());
        try {
            // 1. Global uniqueness check
            String normalizedMobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.getPhoneNumber());
            if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
                throw new IllegalArgumentException("USER_WITH_MOBILE_ALREADY_EXISTS");
            }

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                if (userProfileGrpcClient.getUserByEmail(request.getEmail()) != null) {
                    throw new IllegalArgumentException("USER_WITH_EMAIL_ALREADY_EXISTS");
                }
            }

            String encodedPassword = passwordEncoder.encode(request.getPassword());
            
            // 2. Ensure the role exists
            String roleName = "ROLE_GYM_SUPER_ADMIN";
            roleRepository.findByName(roleName).orElseGet(() -> {
                log.info("Role {} not found, creating it...", roleName);
                Role newRole = new Role();
                newRole.setName(roleName);
                return roleRepository.save(newRole);
            });

            // 3. Create new user
            User user = userService.createNewUser(request.getName(), request.getSurname(), encodedPassword, request.getPhoneNumber());
            
            // 4. Update profile with email
            az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest profileReq = 
                new az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest(request.getName(), request.getSurname(), request.getEmail(), request.getPhoneNumber());
            userService.updateUserProfile(user.getId(), profileReq);
            
            // 5. Assign role
            userService.updateUserRole(user.getId(), "GYM_SUPER_ADMIN");
            
            log.info("Successfully created gym admin with ID: {}", user.getId());
            
            CreateGymAdminResponse response = CreateGymAdminResponse.newBuilder()
                    .setUserId(user.getId())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (az.fitnest.identity.exception.BaseException e) {
            log.warn("Business error during gym admin creation: {}", e.getErrorCode());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getErrorCode())
                    .asRuntimeException());
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for gym admin creation: {}", e.getMessage());
            responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Failed to create gym admin: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create gym admin: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    @Override
    public void checkUserExists(az.fitnest.identity.grpc.CheckUserExistsRequest request, StreamObserver<az.fitnest.identity.grpc.CheckUserExistsResponse> responseObserver) {
        log.info("gRPC: Checking user existence for email: {} and phone: {}", request.getEmail(), request.getPhoneNumber());
        try {
            boolean exists = false;
            String message = "";

            if (!request.getPhoneNumber().isEmpty()) {
                String normalizedMobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.getPhoneNumber());
                if (userRepository.findFirstByMobile(normalizedMobile).isPresent()) {
                    exists = true;
                    message = "USER_WITH_MOBILE_ALREADY_EXISTS";
                }
            }

            if (!exists && !request.getEmail().isEmpty()) {
                if (userProfileGrpcClient.getUserByEmail(request.getEmail()) != null) {
                    exists = true;
                    message = "USER_WITH_EMAIL_ALREADY_EXISTS";
                }
            }

            az.fitnest.identity.grpc.CheckUserExistsResponse response = az.fitnest.identity.grpc.CheckUserExistsResponse.newBuilder()
                    .setExists(exists)
                    .setMessage(message)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to check user existence: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to check user existence: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
