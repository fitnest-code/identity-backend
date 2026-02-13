package az.fitnest.identity.controller;

import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.entity.User;
import az.fitnest.user.grpc.*;
import com.google.protobuf.Empty;
import io.grpc.Status;
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
                    .setLanguage(user.getLanguage() != null ? user.getLanguage() : "")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user: " + e.getMessage())
                    .withCause(e)
                    .asException());
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
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update user profile: " + e.getMessage())
                    .withCause(e)
                    .asException());
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
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update profile image: " + e.getMessage())
                    .withCause(e)
                    .asException());
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
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update setup required: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void updateLanguage(UpdateLanguageRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userService.updateLanguage(request.getUserId(), request.getLanguage());
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update language: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            userService.deleteUser(request.getUserId(), request.getReason());
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete user: " + e.getMessage())
                    .withCause(e)
                    .asException());
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
                .setLanguage(user.getLanguage() != null ? user.getLanguage() : "")
                .setCreatedAt(createdAt)
                .build();
    }
}
