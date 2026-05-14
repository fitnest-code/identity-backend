package az.fitnest.identity.grpc;

import az.fitnest.identity.repository.UserAnalyticsRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * @author: nijataghayev
 */

@GrpcService
@RequiredArgsConstructor
public class IdentityAnalyticsGrpcService
        extends IdentityAnalyticsServiceGrpc.IdentityAnalyticsServiceImplBase {

    private final UserAnalyticsRepository userAnalyticsRepository;

    // ── GetActiveUsersKpi ─────────────────────────────────────────────────────
    @Override
    public void getActiveUsersKpi(
            GetActiveUsersKpiRequest request,
            StreamObserver<ActiveUsersKpiResponse> responseObserver
    ) {
        UserAnalyticsRepository.KpiProjection kpi = userAnalyticsRepository.getActiveUsersKpi();

        responseObserver.onNext(
                ActiveUsersKpiResponse.newBuilder()
                        .setCurrentTotal(kpi.getCurrentTotal())
                        .setPercentageChange(kpi.getPercentageChange())
                        .build()
        );
        responseObserver.onCompleted();
    }

    // ── GetCustomerGrowth ─────────────────────────────────────────────────────
    @Override
    public void getCustomerGrowth(
            GetCustomerGrowthRequest request,
            StreamObserver<CustomerGrowthResponse> responseObserver
    ) {
        boolean isWeekly = "WEEKLY".equalsIgnoreCase(request.getPeriod());

        List<UserAnalyticsRepository.GrowthProjection> rawData = isWeekly
                ? userAnalyticsRepository.getWeeklyCustomerGrowth()
                : userAnalyticsRepository.getMonthlyCustomerGrowth();

        List<GrowthPoint> points = rawData.stream()
                .map(p -> GrowthPoint.newBuilder()
                        .setPeriodLabel(p.getPeriodLabel())
                        .setNewCustomers(p.getNewCustomers())
                        .setActiveCustomers(p.getActiveCustomers())
                        .build())
                .toList();

        responseObserver.onNext(
                CustomerGrowthResponse.newBuilder()
                        .addAllDataPoints(points)
                        .setOverallGrowthPct(calculateOverallGrowth(rawData))
                        .build()
        );
        responseObserver.onCompleted();
    }

    // ── Köməkçi: Ümumi artım faizi ───────────────────────────────────────────
    private double calculateOverallGrowth(List<UserAnalyticsRepository.GrowthProjection> data) {
        if (data.size() < 2) return 0.0;
        long first = data.getFirst().getNewCustomers();
        long last  = data.getLast().getNewCustomers();
        if (first == 0) return 0.0;
        return Math.round(((double) (last - first) / first) * 10000.0) / 100.0;
    }
}
