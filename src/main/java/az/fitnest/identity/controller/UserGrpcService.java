package az.fitnest.identity.controller;

import lombok.extern.slf4j.Slf4j;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest;
import az.fitnest.identity.model.entity.User;
import az.fitnest.user.grpc.UserServiceGrpc;
import az.fitnest.user.grpc.GetUserByIdRequest;
import az.fitnest.user.grpc.UpdateUserProfileRequest;
import az.fitnest.user.grpc.UpdateProfileImageRequest;
import az.fitnest.user.grpc.UpdateSetupRequiredRequest;
import az.fitnest.user.grpc.UpdateLanguageRequest;
import az.fitnest.user.grpc.DeactivateUserRequest;
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

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;

    @Override
    public void getUserById(GetUserByIdRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            User user = userService.getUserById(request.getUserId());
            az.fitnest.user.grpc.UserResponse response = buildUserResponse(user);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getUserById for userId {}: {}", request.getUserId(), e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<az.fitnest.user.grpc.UserResponse> responseObserver) {
        try {
            az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest command = new az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest(
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
    public void deactivateUser(DeactivateUserRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            userService.deactivateUser(request.getUserId(), request.getReason());
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to deactivate user: " + e.getMessage())
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
    public void requestEmailChange(RequestEmailChangeRequest request, StreamObserver<az.fitnest.user.grpc.OtpSendResponseProto> responseObserver) {
        try {
            az.fitnest.identity.dto.response.OtpSendResponse otpResponse = userService.requestEmailChange(request.getUserId(), request.getNewEmail());
            az.fitnest.user.grpc.OtpSendResponseProto response = az.fitnest.user.grpc.OtpSendResponseProto.newBuilder()
                    .setOtpSessionId(otpResponse.otpSessionId() != null ? otpResponse.otpSessionId() : "")
                    .setExpiresInSeconds(otpResponse.expiresInSeconds() != null ? otpResponse.expiresInSeconds() : 0)
                    .setResendAvailableInSeconds(otpResponse.resendAvailableInSeconds() != null ? otpResponse.resendAvailableInSeconds() : 0)
                    .setMessage(otpResponse.message() != null ? otpResponse.message() : "")
                    .build();
            responseObserver.onNext(response);
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
            User user = userService.confirmEmailChange(request.getUserId(), request.getOtpSessionId(), request.getOtpCode());
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
    public void requestMobileChange(RequestMobileChangeRequest request, StreamObserver<az.fitnest.user.grpc.OtpSendResponseProto> responseObserver) {
        try {
            az.fitnest.identity.dto.response.OtpSendResponse otpResponse = userService.requestMobileChange(request.getUserId(), request.getNewMobile());
            az.fitnest.user.grpc.OtpSendResponseProto response = az.fitnest.user.grpc.OtpSendResponseProto.newBuilder()
                    .setOtpSessionId(otpResponse.otpSessionId() != null ? otpResponse.otpSessionId() : "")
                    .setExpiresInSeconds(otpResponse.expiresInSeconds() != null ? otpResponse.expiresInSeconds() : 0)
                    .setResendAvailableInSeconds(otpResponse.resendAvailableInSeconds() != null ? otpResponse.resendAvailableInSeconds() : 0)
                    .setMessage(otpResponse.message() != null ? otpResponse.message() : "")
                    .build();
            responseObserver.onNext(response);
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
            User user = userService.confirmMobileChange(request.getUserId(), request.getOtpSessionId(), request.getOtpCode());
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
                .setFirstName("")
                .setLastName("")
                .setEmail("")
                .setMobile(user.getMobile() != null ? user.getMobile() : "")
                .setProfileImageUrl("")
                .setSetupRequired(user.isSetupRequired())
                .setLanguage(user.getLanguage() != null ? user.getLanguage() : "")
                .setStatus(user.getStatus() != null ? user.getStatus().name() : "")
                .setAccountLocked(user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now()))
                .setSessionStatus(user.getSessionStatus() != null ? user.getSessionStatus().name() : "")
                .setCreatedAt(createdDate)
                .setHasLocalPassword(user.isHasLocalPassword())
                .build();
    }
}
