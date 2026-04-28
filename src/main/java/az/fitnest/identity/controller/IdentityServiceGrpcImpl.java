package az.fitnest.identity.controller;

import az.fitnest.identity.grpc.CreateGymAdminRequest;
import az.fitnest.identity.grpc.CreateGymAdminResponse;
import az.fitnest.identity.grpc.IdentityServiceGrpc;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

@GrpcService
@RequiredArgsConstructor
public class IdentityServiceGrpcImpl extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createGymAdmin(CreateGymAdminRequest request, StreamObserver<CreateGymAdminResponse> responseObserver) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userService.createNewUser(request.getName(), request.getSurname(), encodedPassword, request.getPhoneNumber());
        
        az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest profileReq = new az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest(request.getName(), request.getSurname(), request.getEmail(), request.getPhoneNumber());
        userService.updateUserProfile(user.getId(), profileReq);
        
        userService.updateUserRole(user.getId(), "GYM_SUPER_ADMIN");
        
        CreateGymAdminResponse response = CreateGymAdminResponse.newBuilder()
                .setUserId(user.getId())
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
