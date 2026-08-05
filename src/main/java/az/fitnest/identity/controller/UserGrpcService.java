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

import az.fitnest.identity.repository.UserRepository;
import az.fitnest.user.grpc.SearchUserIdsByMobileRequest;
import az.fitnest.user.grpc.SearchUserIdsByMobileResponse;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService userService;
    private final UserRepository userRepository;

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
    public void getUsersByIds(az.fitnest.user.grpc.GetUsersByIdsRequest request, StreamObserver<az.fitnest.user.grpc.GetUsersByIdsResponse> responseObserver) {
        try {
            java.util.List<User> users = userRepository.findAllById(request.getUserIdsList());
            az.fitnest.user.grpc.GetUsersByIdsResponse.Builder responseBuilder = az.fitnest.user.grpc.GetUsersByIdsResponse.newBuilder();
            for (User user : users) {
                responseBuilder.addUsers(buildUserResponse(user));
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getUsersByIds: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get users: " + e.getMessage())
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

    @Override
    public void searchUserIdsByMobile(SearchUserIdsByMobileRequest request, StreamObserver<SearchUserIdsByMobileResponse> responseObserver) {
        try {
            String query = request.getQuery();
            log.info("Searching user IDs by mobile query: {}", query);
            
            // Clean query to only digits for robust matching
            String cleanQuery = query != null ? query.replaceAll("\\D", "") : "";
            if (cleanQuery.startsWith("0") && cleanQuery.length() > 1) {
                cleanQuery = cleanQuery.substring(1);
            } else if (cleanQuery.startsWith("994") && cleanQuery.length() > 3) {
                cleanQuery = cleanQuery.substring(3);
            }
            
            java.util.List<Long> userIds;
            if (!cleanQuery.isEmpty()) {
                userIds = userRepository.findUserIdsByMobileContaining(cleanQuery);
            } else {
                userIds = java.util.Collections.emptyList();
            }
            
            SearchUserIdsByMobileResponse response = SearchUserIdsByMobileResponse.newBuilder()
                    .addAllUserIds(userIds)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in searchUserIdsByMobile: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to search users by mobile: " + e.getMessage())
                    .withCause(e)
                    .asException());
        }
    }

    @Override
    public void getUserIdsByRoles(az.fitnest.user.grpc.GetUserIdsByRolesRequest request, io.grpc.stub.StreamObserver<az.fitnest.user.grpc.GetUserIdsByRolesResponse> responseObserver) {
        try {
            java.util.List<String> roleNames = request.getRoleNamesList();
            log.info("gRPC: Fetching user IDs for roles: {}", roleNames);
            java.util.List<Long> userIds;
            if (roleNames.contains("ROLE_USER")) {
                userIds = userRepository.findUserIdsByRoleNamesOrPartnersWithMobile(roleNames, java.util.List.of("ROLE_GYM_SUPER_ADMIN", "ROLE_GYM_ADMIN", "ROLE_PARTNER"));
            } else {
                userIds = userRepository.findUserIdsByRoleNames(roleNames);
            }
            az.fitnest.user.grpc.GetUserIdsByRolesResponse response = az.fitnest.user.grpc.GetUserIdsByRolesResponse.newBuilder()
                    .addAllUserIds(userIds)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getUserIdsByRoles: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to get user IDs by roles: " + e.getMessage())
                    .asException());
        }
    }

    @Override
    public void getActiveUsersWithLanguage(az.fitnest.user.grpc.GetActiveUsersWithLanguageRequest request,
                                           StreamObserver<az.fitnest.user.grpc.GetActiveUsersWithLanguageResponse> responseObserver) {
        try {
            java.util.List<String> roleNames = request.getRoleNamesList();
            if (roleNames == null || roleNames.isEmpty()) {
                roleNames = java.util.List.of("ROLE_USER");
            }
            // "Active users" for fan-out = all users with ROLE_USER (exclude only hard-deleted)
            java.util.List<Object[]> rows = userRepository.findUserIdsAndLanguagesByRoles(roleNames);
            az.fitnest.user.grpc.GetActiveUsersWithLanguageResponse.Builder responseBuilder =
                    az.fitnest.user.grpc.GetActiveUsersWithLanguageResponse.newBuilder();
            for (Object[] row : rows) {
                Long userId = (Long) row[0];
                String language = row[1] != null ? row[1].toString() : "AZ";
                responseBuilder.addUsers(az.fitnest.user.grpc.ActiveUserLanguage.newBuilder()
                        .setUserId(userId)
                        .setLanguage(language)
                        .build());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getActiveUsersWithLanguage: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get active users with language: " + e.getMessage())
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
                .setRole(user.getRole() != null ? user.getRole().getName() : "")
                .build();
    }
}
