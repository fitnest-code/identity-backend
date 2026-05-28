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
        try {
            String normalizedMobile = az.fitnest.identity.util.MobileNumberUtils.normalize(request.getPhoneNumber());
            User user = userRepository.findFirstByMobile(normalizedMobile).orElse(null);

            if (user == null) {
                String encodedPassword = passwordEncoder.encode(request.getPassword());
                user = userService.createNewUser(request.getName(), request.getSurname(), encodedPassword, request.getPhoneNumber());

                var profileReq = new az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest(
                        request.getName(), request.getSurname(), request.getEmail(), request.getPhoneNumber());
                userService.updateUserProfile(user.getId(), profileReq);
                log.info("Created new gym admin user with ID: {}", user.getId());
            } else {
                log.info("Existing user found by mobile, reusing ID: {}", user.getId());
                user.setStatus(az.fitnest.identity.model.enums.UserStatus.ACTIVE);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);

                var profileReq = new az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest(
                        request.getName(), request.getSurname(), request.getEmail(), request.getPhoneNumber());
                userService.updateUserProfile(user.getId(), profileReq);
            }

            ensureRoleExists("ROLE_GYM_SUPER_ADMIN");
            userService.updateUserRole(user.getId(), "GYM_SUPER_ADMIN");

            responseObserver.onNext(CreateGymAdminResponse.newBuilder().setUserId(user.getId()).build());
            responseObserver.onCompleted();
        } catch (az.fitnest.identity.exception.BaseException e) {
            log.warn("Business error during gym admin creation: {}", e.getErrorCode());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getErrorCode()).asRuntimeException());
        } catch (Exception e) {
            log.error("Failed to create gym admin: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to create gym admin: " + e.getMessage()).asRuntimeException());
        }
    }

    private void ensureRoleExists(String roleName) {
        roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            return roleRepository.save(role);
        });
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
