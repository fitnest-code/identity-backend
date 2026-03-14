package az.fitnest.identity.service;

import az.fitnest.user.grpc.GetUserProfileDetailsRequest;
import az.fitnest.user.grpc.UserProfileDetailsResponse;
import az.fitnest.user.grpc.UserProfileServiceGrpc;
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
        GetUserProfileDetailsRequest request = GetUserProfileDetailsRequest.newBuilder()
                .setUserId(userId)
                .build();
        UserProfileDetailsResponse response = stub.getUserProfileDetails(request);
        channel.shutdown();
        return response;
    }
}
