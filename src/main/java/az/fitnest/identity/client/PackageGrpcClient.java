package az.fitnest.identity.client;

import az.fitnest.order.grpc.UserSubscriptionServiceGrpc;
import az.fitnest.order.grpc.GetUserIdsByPackageIdRequest;
import az.fitnest.order.grpc.GetUserIdsByPackageIdResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackageGrpcClient {
    @GrpcClient("order-service")
    private UserSubscriptionServiceGrpc.UserSubscriptionServiceBlockingStub blockingStub;

    public List<Long> getUserIdsByPackageId(Long packageId) {
        GetUserIdsByPackageIdRequest request = GetUserIdsByPackageIdRequest.newBuilder()
                .setPackageId(packageId)
                .build();
        GetUserIdsByPackageIdResponse response = blockingStub.getUserIdsByPackageId(request);
        return response.getUserIdsList();
    }
}

