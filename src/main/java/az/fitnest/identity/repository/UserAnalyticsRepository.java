package az.fitnest.identity.repository;

import az.fitnest.identity.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: nijataghayev
 */

@Repository
public interface UserAnalyticsRepository extends JpaRepository<User, Long> {

    // ── KPI: cari ay aktiv user sayı və əvvəlki aya nisbət artım faizi ──────
    @Query(value = """
            WITH current_month AS (
                SELECT COUNT(*) AS cnt
                FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE u.status = 'ACTIVE'
                  AND r.name   = 'ROLE_USER'
                  AND u.created_at >= date_trunc('month', CURRENT_DATE)
            ),
            previous_month AS (
                SELECT COUNT(*) AS cnt
                FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE u.status = 'ACTIVE'
                  AND r.name   = 'ROLE_USER'
                  AND u.created_at >= date_trunc('month', CURRENT_DATE - INTERVAL '1 month')
                  AND u.created_at <  date_trunc('month', CURRENT_DATE)
            )
            SELECT
                (SELECT cnt FROM current_month) AS current_total,
                CASE
                    WHEN (SELECT cnt FROM previous_month) = 0 THEN 0.0
                    ELSE ROUND(
                        ((SELECT cnt FROM current_month) - (SELECT cnt FROM previous_month))::numeric
                        / (SELECT cnt FROM previous_month) * 100.0,
                    2)
                END AS percentage_change
            """, nativeQuery = true)
    KpiProjection getActiveUsersKpi();

    // ── Customer Growth: Aylıq bölgü (cari il) ───────────────────────────────
    @Query(value = """
            SELECT
                to_char(date_trunc('month', u.created_at), 'Mon') AS period_label,
                COUNT(*) FILTER (
                    WHERE u.created_at >= date_trunc('month', u.created_at)
                      AND u.created_at <  date_trunc('month', u.created_at) + INTERVAL '1 month'
                ) AS new_customers,
                COUNT(*) FILTER (
                    WHERE u.status = 'ACTIVE'
                ) AS active_customers
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.created_at >= date_trunc('year', CURRENT_DATE)
              AND r.name = 'ROLE_USER'
            GROUP BY date_trunc('month', u.created_at)
            ORDER BY date_trunc('month', u.created_at)
            """, nativeQuery = true)
    List<GrowthProjection> getMonthlyCustomerGrowth();

    // ── Customer Growth: Həftəlik bölgü (son 12 həftə) ───────────────────────
    @Query(value = """
            SELECT
                'W' || to_char(date_trunc('week', u.created_at), 'IW') AS period_label,
                COUNT(*) FILTER (
                    WHERE u.created_at >= date_trunc('week', u.created_at)
                      AND u.created_at <  date_trunc('week', u.created_at) + INTERVAL '1 week'
                ) AS new_customers,
                COUNT(*) FILTER (
                    WHERE u.status = 'ACTIVE'
                ) AS active_customers
            FROM users u
            JOIN roles r ON r.id = u.role_id
            WHERE u.created_at >= CURRENT_DATE - INTERVAL '12 weeks'
              AND r.name = 'ROLE_USER'
            GROUP BY date_trunc('week', u.created_at)
            ORDER BY date_trunc('week', u.created_at)
            """, nativeQuery = true)
    List<GrowthProjection> getWeeklyCustomerGrowth();

    // ── Projection interface-ləri ─────────────────────────────────────────────
    interface KpiProjection {
        Long   getCurrentTotal();
        Double getPercentageChange();
    }

    interface GrowthProjection {
        String getPeriodLabel();
        Long   getNewCustomers();
        Long   getActiveCustomers();
    }
}