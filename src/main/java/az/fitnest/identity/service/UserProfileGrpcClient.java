package az.fitnest.identity.service;

import az.fitnest.user.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

@Service
public class UserProfileGrpcClient {
    public UserProfileDetailsResponse getUserProfileDetails(Long userId) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("user-service", 9090)
                .usePlaintext()
                .build();
        UserProfileServiceGrpc.UserProfileServiceBlockingStub stub = UserProfileServiceGrpc.newBlockingStub(channel);
        try {
            UserProfileDetailsRequest request = UserProfileDetailsRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return stub.getUserProfileDetails(request);
        } finally {
            channel.shutdown();
        }
    }

    public void updateProfileImage(Long userId, String imageUrl) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("user-service", 9090)
                .usePlaintext()
                .build();
        UserProfileServiceGrpc.UserProfileServiceBlockingStub stub = UserProfileServiceGrpc.newBlockingStub(channel);
        try {
            UpdateProfileImageDetailsRequest request = UpdateProfileImageDetailsRequest.newBuilder()
                    .setUserId(userId)
                    .setImageUrl(imageUrl != null ? imageUrl : "")
                    .build();
            stub.updateProfileImage(request);
        } finally {
            channel.shutdown();
        }
    }

    public UserByEmailResponse getUserByEmail(String email) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("user-service", 9090)
                .usePlaintext()
                .build();
        UserProfileServiceGrpc.UserProfileServiceBlockingStub stub = UserProfileServiceGrpc.newBlockingStub(channel);
        try {
            UserByEmailRequest request = UserByEmailRequest.newBuilder()
                    .setEmail(email)
                    .build();
            UserProfileDetailsResponse response = stub.getUserByEmail(request);
            return new UserByEmailResponse(response.getUserId(), response.getFirstName(), response.getLastName(), response.getEmail());
        } catch (Exception e) {
            return null;
        } finally {
            channel.shutdown();
        }
    }

    public void createUserProfile(Long userId, String firstName, String lastName, String email) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("user-service", 9090)
                .usePlaintext()
                .build();
        UserProfileServiceGrpc.UserProfileServiceBlockingStub stub = UserProfileServiceGrpc.newBlockingStub(channel);
        try {
            CreateProfileRequest request = CreateProfileRequest.newBuilder()
                    .setUserId(userId)
                    .setFirstName(firstName != null ? firstName : "")
                    .setLastName(lastName != null ? lastName : "")
                    .setEmail(email != null ? email : "")
                    .build();
            stub.createUserProfile(request);
        } finally {
            channel.shutdown();
        }
    }

    public record UserByEmailResponse(Long userId, String firstName, String lastName, String email) {}
}
