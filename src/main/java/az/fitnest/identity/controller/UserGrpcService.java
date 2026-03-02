package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.model.entity.User;
import az.fitnest.user.grpc.UserServiceGrpc;
import az.fitnest.user.grpc.GetUserByIdRequest;
import az.fitnest.user.grpc.UpdateUserProfileRequest;
import az.fitnest.user.grpc.UpdateProfileImageRequest;
import az.fitnest.user.grpc.UpdateSetupRequiredRequest;
import az.fitnest.user.grpc.UpdateLanguageRequest;
import az.fitnest.user.grpc.DeleteUserRequest;
import az.fitnest.user.grpc.UpdateSessionStatusRequest;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import az.fitnest.user.grpc.RequestEmailChangeRequest;
import az.fitnest.user.grpc.ConfirmEmailChangeRequest;
import az.fitnest.user.grpc.RequestMobileChangeRequest;
import az.fitnest.user.grpc.ConfirmMobileChangeRequest;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    @Override
    public void getUserById(GetUserByIdRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.getUserById(request.getUserId());
            az.fitnest.user.grpc.UserResponse response = az.fitnest.user.grpc.UserResponse.newBuilder()
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
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            az.fitnest.identity.dto.UpdateUserProfileCommand command = new az.fitnest.identity.dto.UpdateUserProfileCommand(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getMobile()
            );

            User user = userService.updateUserProfile(request.getUserId(), command);
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);

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
    public void updateProfileImage(UpdateProfileImageRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.updateProfileImageUrl(request.getUserId(), request.getImageUrl());
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);

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
    public void updateSetupRequired(UpdateSetupRequiredRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.updateSetupRequired(request.getUserId(), request.getSetupRequired());
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);

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
    public void updateLanguage(UpdateLanguageRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.updateLanguage(request.getUserId(), request.getLanguage());
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);

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

    @Override
    public void updateSessionStatus(UpdateSessionStatusRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            az.fitnest.identity.model.enums.SessionStatus sessionStatus = az.fitnest.identity.model.enums.SessionStatus.valueOf(request.getSessionStatus());
            User user = userService.updateSessionStatus(request.getUserId(), sessionStatus);
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update session status: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void requestEmailChange(RequestEmailChangeRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            userService.requestEmailChange(request.getUserId(), request.getNewEmail());
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to request email change: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void confirmEmailChange(ConfirmEmailChangeRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.confirmEmailChange(request.getUserId(), request.getNewEmail(), request.getOtpCode());
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to confirm email change: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void requestMobileChange(RequestMobileChangeRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            userService.requestMobileChange(request.getUserId(), request.getNewMobile());
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to request mobile change: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void confirmMobileChange(ConfirmMobileChangeRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.confirmMobileChange(request.getUserId(), request.getNewMobile(), request.getOtpCode());
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to confirm mobile change: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    private az.fitnest.user.grpc.UserResponse buildUserResponse(User user) {
        String createdDate = user.getCreatedDate() != null ? user.getCreatedDate().toString() : "";
        return az.fitnest.user.grpc.UserResponse.newBuilder()
                .setUserId(user.getId())
                .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                .setLastName(user.getLastName() != null ? user.getLastName() : "")
                .setEmail(user.getEmail() != null ? user.getEmail() : "")
                .setMobile(user.getMobile() != null ? user.getMobile() : "")
                .setProfileImageUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "")
                .setSetupRequired(user.isSetupRequired())
                .setLanguage(user.getLanguage() != null ? user.getLanguage() : "")
                .setStatus(user.getStatus() != null ? user.getStatus().name() : "")
                .setAccountLocked(user.isAccountLocked())
                .setSessionStatus(user.getSessionStatus() != null ? user.getSessionStatus().name() : "")
                .setCreatedAt(createdDate)
                .build();
    }
}
