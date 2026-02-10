package az.fitnest.identity.grpc;

import az.fitnest.identity.user.adapter.service.UserService;
import az.fitnest.identity.user.api.dto.request.UpdateUserProfileCommand;
import az.fitnest.identity.user.domain.enums.Language;
import az.fitnest.identity.user.domain.model.User;
import az.fitnest.user.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    @Override
    public void getUserById(GetUserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userService.getUserById(request.getUserId());
            UserResponse response = UserResponse.newBuilder()
                    .setUserId(user.getId())
                    .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                    .setLastName(user.getLastName() != null ? user.getLastName() : "")
                    .setEmail(user.getEmail() != null ? user.getEmail() : "")
                    .setMobile(user.getMobile() != null ? user.getMobile() : "")
                    .setProfileImageUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "")
                    .setSetupRequired(user.isSetupRequired())
                    .setLanguage(user.getLanguage() != null ? user.getLanguage().name() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            UpdateUserProfileCommand command = UpdateUserProfileCommand.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .build();

            User user = userService.updateUserProfile(request.getUserId(), command);
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateProfileImage(UpdateProfileImageRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userService.updateProfileImageUrl(request.getUserId(), request.getImageUrl());
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateSetupRequired(UpdateSetupRequiredRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userService.updateSetupRequired(request.getUserId(), request.getSetupRequired());
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateLanguage(UpdateLanguageRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            Language language = Language.valueOf(request.getLanguage().toUpperCase());
            User user = userService.updateLanguage(request.getUserId(), language);
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
        try {
            userService.deleteUser(request.getUserId(), request.getReason());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private UserResponse buildUserResponse(User user) {
        String createdAt = user.getCreatedDate() != null ? user.getCreatedDate().toString() : "";
        return UserResponse.newBuilder()
                .setUserId(user.getId())
                .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                .setLastName(user.getLastName() != null ? user.getLastName() : "")
                .setEmail(user.getEmail() != null ? user.getEmail() : "")
                .setMobile(user.getMobile() != null ? user.getMobile() : "")
                .setProfileImageUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "")
                .setSetupRequired(user.isSetupRequired())
                .setLanguage(user.getLanguage() != null ? user.getLanguage().name() : "")
                .setCreatedAt(createdAt)
                .build();
    }
}
