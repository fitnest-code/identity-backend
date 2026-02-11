package az.fitnest.identity.controller;

import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.constants.Language;
import az.fitnest.identity.entity.User;
import az.fitnest.user.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    @Override
    public void getUserById(GetUserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            log.debug("gRPC: Getting user by id: {}", request.getUserId());
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
            log.error("gRPC Error in getUserById: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            log.debug("gRPC: Updating user profile for userId: {}", request.getUserId());
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
            log.error("gRPC Error in updateUserProfile: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update user profile: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void updateProfileImage(UpdateProfileImageRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            log.debug("gRPC: Updating profile image for userId: {}", request.getUserId());
            User user = userService.updateProfileImageUrl(request.getUserId(), request.getImageUrl());
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Error in updateProfileImage: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update profile image: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void updateSetupRequired(UpdateSetupRequiredRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            log.debug("gRPC: Updating setup required status for userId: {}, setupRequired: {}",
                    request.getUserId(), request.getSetupRequired());
            User user = userService.updateSetupRequired(request.getUserId(), request.getSetupRequired());
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.debug("gRPC: Successfully updated setup required status for userId: {}", request.getUserId());
        } catch (Exception e) {
            log.error("gRPC Error in updateSetupRequired: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update setup required: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void updateLanguage(UpdateLanguageRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            log.debug("gRPC: Updating language for userId: {}, language: {}", request.getUserId(), request.getLanguage());
            Language language = Language.valueOf(request.getLanguage().toUpperCase());
            User user = userService.updateLanguage(request.getUserId(), language);
            UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("gRPC Error in updateLanguage - Invalid language: {}", request.getLanguage(), e);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid language: " + request.getLanguage())
                    .withCause(e)
                    .asException());
        } catch (Exception e) {
            log.error("gRPC Error in updateLanguage: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update language: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("gRPC: Deleting user with userId: {}, reason: {}", request.getUserId(), request.getReason());
            userService.deleteUser(request.getUserId(), request.getReason());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
            log.debug("gRPC: Successfully deleted user with userId: {}", request.getUserId());
        } catch (Exception e) {
            log.error("gRPC Error in deleteUser: {}", e.getMessage(), e);
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
                .setLanguage(user.getLanguage() != null ? user.getLanguage().name() : "")
                .setCreatedAt(createdAt)
                .build();
    }
}
