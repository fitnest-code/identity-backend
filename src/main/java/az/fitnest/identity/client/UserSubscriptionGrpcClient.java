package az.fitnest.identity.client;

import az.fitnest.order.grpc.UserSubscriptionServiceGrpc;
import az.fitnest.order.grpc.GetUserIdsByPackageIdRequest;
import az.fitnest.order.grpc.GetUserIdsByPackageIdResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserSubscriptionGrpcClient {
    private final UserSubscriptionServiceGrpc.UserSubscriptionServiceBlockingStub stub;

    public UserSubscriptionGrpcClient(@Value("${order.service.grpc.host}") String host,
                                      @Value("${order.service.grpc.port}") int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = UserSubscriptionServiceGrpc.newBlockingStub(channel);
    }

    public List<Long> getUserIdsByPackageId(long packageId) {
        GetUserIdsByPackageIdRequest request = GetUserIdsByPackageIdRequest.newBuilder()
                .setPackageId(packageId)
                .build();
        GetUserIdsByPackageIdResponse response = stub.getUserIdsByPackageId(request);
        return response.getUserIdsList().stream().map(Long::valueOf).collect(Collectors.toList());
    }
}
