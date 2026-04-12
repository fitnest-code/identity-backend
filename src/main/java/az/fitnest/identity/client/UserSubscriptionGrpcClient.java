package az.fitnest.identity.client;

import az.fitnest.order.grpc.UserSubscriptionServiceGrpc;
import az.fitnest.order.grpc.GetUserIdsByPackageIdRequest;
import az.fitnest.order.grpc.GetUserIdsByPackageIdResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserSubscriptionGrpcClient {

    @GrpcClient("order-backend")
    private UserSubscriptionServiceGrpc.UserSubscriptionServiceBlockingStub stub;

    public List<Long> getUserIdsByPackageId(long packageId) {
        GetUserIdsByPackageIdRequest request = GetUserIdsByPackageIdRequest.newBuilder()
                .setPackageId(packageId)
                .build();
        GetUserIdsByPackageIdResponse response = stub.getUserIdsByPackageId(request);
        return response.getUserIdsList().stream().map(Long::valueOf).collect(Collectors.toList());
    }

    public List<Long> getUserIdsByDurationMonths(int durationMonths) {
        az.fitnest.order.grpc.GetUserIdsByDurationMonthsRequest request = az.fitnest.order.grpc.GetUserIdsByDurationMonthsRequest.newBuilder()
                .setDurationMonths(durationMonths)
                .build();
        az.fitnest.order.grpc.GetUserIdsByPackageIdResponse response = stub.getUserIdsByDurationMonths(request);
        return response.getUserIdsList().stream().map(Long::valueOf).collect(Collectors.toList());
    }

    public List<Long> getUserIdsByType(String type) {
        az.fitnest.order.grpc.GetUserIdsByTypeRequest request = az.fitnest.order.grpc.GetUserIdsByTypeRequest.newBuilder()
                .setType(type)
                .build();
        az.fitnest.order.grpc.GetUserIdsByPackageIdResponse response = stub.getUserIdsByType(request);
        return response.getUserIdsList().stream().map(Long::valueOf).collect(Collectors.toList());
    }

    public az.fitnest.order.grpc.ActiveSubscriptionResponse getActiveSubscription(Long userId) {
        az.fitnest.order.grpc.GetActiveSubscriptionRequest request = az.fitnest.order.grpc.GetActiveSubscriptionRequest.newBuilder()
                .setUserId(userId)
                .build();
        return stub.getActiveSubscription(request);
    }
}
