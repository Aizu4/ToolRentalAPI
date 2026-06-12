package tool.rental.api.controllers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tool.rental.api.annotations.AdminOnly;
import tool.rental.api.entities.RentalStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/stats")
@AdminOnly
public class StatsController {

    private static final int ACTIVE_BUCKETS = 180;
    private static final long MAX_ACTIVE_WINDOW_MINUTES = 60L * 24L * 365L;

    @PersistenceContext
    private EntityManager em;

    private static final Set<RentalStatus> ACTIVE_STATUSES =
            Set.of(RentalStatus.PENDING, RentalStatus.SENT, RentalStatus.DELIVERED);

    private static final List<RentalStatus> TERMINAL_STATUSES =
            List.of(RentalStatus.RETURNED, RentalStatus.CANCELLED, RentalStatus.LOST);

    public record MonthStatusCount(String month, RentalStatus status, long count) {}
    public record StatusTimePoint(String ts, RentalStatus status, long count) {}

    @GetMapping("/rentals-per-month")
    public List<MonthStatusCount> rentalsPerMonth(@RequestParam(defaultValue = "12") int months) {
        int n = Math.max(1, Math.min(months, 60));
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(n - 1L);
        ZoneId zone = ZoneId.systemDefault();
        Instant from = start.atDay(1).atStartOfDay(zone).toInstant();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT u.status, u.createdAt FROM ItemRentalUpdate u " +
                        "WHERE u.status IN :terminals AND u.createdAt >= :from"
        ).setParameter("terminals", TERMINAL_STATUSES)
                .setParameter("from", from)
                .getResultList();

        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Map<RentalStatus, Long>> bins = new HashMap<>();
        for (Object[] row : rows) {
            RentalStatus status = (RentalStatus) row[0];
            Instant at = (Instant) row[1];
            String ym = YearMonth.from(at.atZone(zone).toLocalDate()).format(ymFmt);
            bins.computeIfAbsent(ym, k -> new EnumMap<>(RentalStatus.class))
                    .merge(status, 1L, Long::sum);
        }

        List<MonthStatusCount> result = new ArrayList<>(n * TERMINAL_STATUSES.size());
        for (int i = 0; i < n; i++) {
            String key = start.plusMonths(i).format(ymFmt);
            Map<RentalStatus, Long> mb = bins.getOrDefault(key, Collections.emptyMap());
            for (RentalStatus s : TERMINAL_STATUSES) {
                result.add(new MonthStatusCount(key, s, mb.getOrDefault(s, 0L)));
            }
        }
        return result;
    }

    @GetMapping("/active-rentals-over-time")
    public List<StatusTimePoint> activeRentalsOverTime(@RequestParam(defaultValue = "10080") long minutes) {
        long m = Math.max(1L, Math.min(minutes, MAX_ACTIVE_WINDOW_MINUTES));
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofMinutes(m));

        // Pull every audit row up to the window's end, ordered chronologically per rental.
        // Status at any bucket time = last update with createdAt <= bucket.
        @SuppressWarnings("unchecked")
        List<Object[]> raw = em.createQuery(
                "SELECT u.rental.id, u.status, u.createdAt FROM ItemRentalUpdate u " +
                        "WHERE u.createdAt <= :end ORDER BY u.rental.id, u.createdAt"
        ).setParameter("end", end).getResultList();

        Map<Long, List<Object[]>> updatesByRental = new HashMap<>();
        for (Object[] row : raw) {
            updatesByRental.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add(row);
        }

        long stepMillis = Duration.ofMinutes(m).toMillis() / ACTIVE_BUCKETS;
        if (stepMillis < 1) stepMillis = 1;

        List<RentalStatus> series = List.of(RentalStatus.PENDING, RentalStatus.SENT, RentalStatus.DELIVERED);
        List<StatusTimePoint> result = new ArrayList<>(ACTIVE_BUCKETS * series.size());

        for (int i = 0; i < ACTIVE_BUCKETS; i++) {
            Instant bucket = start.plusMillis(stepMillis * i);
            Map<RentalStatus, Long> counts = new EnumMap<>(RentalStatus.class);

            for (List<Object[]> updates : updatesByRental.values()) {
                RentalStatus current = null;
                for (Object[] u : updates) {
                    Instant at = (Instant) u[2];
                    if (at.isAfter(bucket)) break;
                    current = (RentalStatus) u[1];
                }
                if (current != null && ACTIVE_STATUSES.contains(current)) {
                    counts.merge(current, 1L, Long::sum);
                }
            }

            String ts = bucket.toString();
            for (RentalStatus s : series) {
                result.add(new StatusTimePoint(ts, s, counts.getOrDefault(s, 0L)));
            }
        }
        return result;
    }
}
