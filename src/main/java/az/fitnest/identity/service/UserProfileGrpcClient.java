package az.fitnest.identity.service;

import az.fitnest.user.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class UserProfileGrpcClient {

    @GrpcClient("user-service")
    private UserProfileServiceGrpc.UserProfileServiceBlockingStub stub;

    public UserProfileDetailsResponse getUserProfileDetails(Long userId) {
        UserProfileDetailsRequest request = UserProfileDetailsRequest.newBuilder()
                .setUserId(userId)
                .build();
        return stub.getUserProfileDetails(request);
    }

    public void updateProfileImage(Long userId, String imageUrl) {
        UpdateProfileImageDetailsRequest request = UpdateProfileImageDetailsRequest.newBuilder()
                .setUserId(userId)
                .setImageUrl(imageUrl != null ? imageUrl : "")
                .build();
        stub.updateProfileImage(request);
    }

    public UserByEmailResponse getUserByEmail(String email) {
        try {
            UserByEmailRequest request = UserByEmailRequest.newBuilder()
                    .setEmail(email)
                    .build();
            UserProfileDetailsResponse response = stub.getUserByEmail(request);
            return new UserByEmailResponse(response.getUserId(), response.getFirstName(), response.getLastName(), response.getEmail());
        } catch (Exception e) {
            return null;
        }
    }

    public void createUserProfile(Long userId, String firstName, String lastName, String email) {
        CreateProfileRequest request = CreateProfileRequest.newBuilder()
                .setUserId(userId)
                .setFirstName(firstName != null ? firstName : "")
                .setLastName(lastName != null ? lastName : "")
                .setEmail(email != null ? email : "")
                .build();
        stub.createUserProfile(request);
    }

    public record UserByEmailResponse(Long userId, String firstName, String lastName, String email) {}
}
