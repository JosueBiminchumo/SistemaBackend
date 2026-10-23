package com.mediturno.backend.statistics.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mediturno.backend.statistics.dto.AppointmentSeriesResponse;
import com.mediturno.backend.statistics.dto.DashboardStatisticsResponse;
import com.mediturno.backend.statistics.repository.StatisticsBucket;
import com.mediturno.backend.statistics.repository.StatisticsTotals;
import com.mediturno.backend.statistics.service.StatisticsGrouping;
import com.mediturno.backend.statistics.service.StatisticsRange;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatisticsMapperTest {

    private final StatisticsMapper mapper = new StatisticsMapper();
    private final StatisticsRange range = new StatisticsRange(
            LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1));

    @Test
    void fillsMissingDaysAcrossLeapDayAndKeepsChartValuesAligned() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.DAY, range, List.of(
                new StatisticsBucket("2024-03-01", 5),
                new StatisticsBucket("2024-02-28", 2)));

        assertThat(response.labels()).containsExactly("2024-02-28", "2024-02-29", "2024-03-01");
        assertThat(response.values()).containsExactly(2L, 0L, 5L);
        assertThat(response.groupBy()).isEqualTo("day");
        assertThat(response.timeZone()).isEqualTo("UTC");
    }

    @Test
    void emptyDailyDataStillProducesEveryInclusiveDate() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.DAY, range, List.of());

        assertThat(response.labels()).containsExactly("2024-02-28", "2024-02-29", "2024-03-01");
        assertThat(response.values()).containsExactly(0L, 0L, 0L);
    }

    @Test
    void mergesRepeatedDailyBucketsWithoutAddingDuplicateChartLabels() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.DAY, range, List.of(
                new StatisticsBucket("2024-02-29", 3),
                new StatisticsBucket("2024-02-29", 4)));

        assertThat(response.labels()).containsExactly("2024-02-28", "2024-02-29", "2024-03-01");
        assertThat(response.values()).containsExactly(0L, 7L, 0L);
    }

    @Test
    void statusSeriesFillsKnownStatesAndPreservesFutureStateData() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.STATUS, range, List.of(
                new StatisticsBucket("NO_SHOW", 2),
                new StatisticsBucket("CONFIRMED", 7),
                new StatisticsBucket("NO_SHOW", 3)));

        assertThat(response.labels()).containsExactly(
                "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED", "NO_SHOW");
        assertThat(response.values()).containsExactly(0L, 7L, 0L, 0L, 5L);
    }

    @Test
    void emptyStatusSeriesKeepsStableChartCategoriesAtZero() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.STATUS, range, List.of());

        assertThat(response.labels()).containsExactly("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED");
        assertThat(response.values()).containsExactly(0L, 0L, 0L, 0L);
    }

    @Test
    void specialtySeriesPreservesRepositoryOrderAndMatchingCounts() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.SPECIALTY, range, List.of(
                new StatisticsBucket("Traumatologia", 9),
                new StatisticsBucket("Cardiologia", 4),
                new StatisticsBucket("Pediatria", 1)));

        assertThat(response.labels()).containsExactly("Traumatologia", "Cardiologia", "Pediatria");
        assertThat(response.values()).containsExactly(9L, 4L, 1L);
    }

    @Test
    void emptySpecialtySeriesDoesNotInventCategories() {
        AppointmentSeriesResponse response = mapper.series(StatisticsGrouping.SPECIALTY, range, List.of());

        assertThat(response.labels()).isEmpty();
        assertThat(response.values()).isEmpty();
    }

    @Test
    void dashboardCombinesGlobalTotalsWithCompleteStatusBreakdown() {
        DashboardStatisticsResponse response = mapper.dashboard(range,
                new StatisticsTotals(8, 25, 4, 3), List.of(
                        new StatisticsBucket("COMPLETED", 5),
                        new StatisticsBucket("PENDING", 3)));

        assertThat(response.totalAppointments()).isEqualTo(8);
        assertThat(response.totalPatients()).isEqualTo(25);
        assertThat(response.totalDoctors()).isEqualTo(4);
        assertThat(response.totalSpecialties()).isEqualTo(3);
        assertThat(response.appointmentsByStatus().keySet())
                .containsExactly("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED");
        assertThat(response.appointmentsByStatus().values()).containsExactly(3L, 0L, 0L, 5L);
        assertThat(response.from()).isEqualTo(range.from());
        assertThat(response.to()).isEqualTo(range.to());
        assertThat(response.timeZone()).isEqualTo("UTC");
    }

    @Test
    void dashboardDoesNotDiscardStatusesAddedByAnotherModule() {
        DashboardStatisticsResponse response = mapper.dashboard(range,
                new StatisticsTotals(2, 1, 1, 1), List.of(new StatisticsBucket("NO_SHOW", 2)));

        assertThat(response.appointmentsByStatus().keySet())
                .containsExactly("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED", "NO_SHOW");
        assertThat(response.appointmentsByStatus()).containsEntry("NO_SHOW", 2L);
    }

    @Test
    void seriesResponseSnapshotsMutableInputsToProtectLabelValueAlignment() {
        List<String> labels = new ArrayList<>(List.of("PENDING", "CONFIRMED"));
        List<Long> values = new ArrayList<>(List.of(1L, 2L));
        AppointmentSeriesResponse response = new AppointmentSeriesResponse(
                "status", range.from(), range.to(), "UTC", labels, values);

        labels.clear();
        values.set(0, 99L);

        assertThat(response.labels()).containsExactly("PENDING", "CONFIRMED");
        assertThat(response.values()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> response.labels().add("CANCELLED"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.values().set(0, 10L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void dashboardResponseSnapshotsMutableStatusMapAndPreservesItsOrder() {
        Map<String, Long> statuses = new LinkedHashMap<>();
        statuses.put("PENDING", 1L);
        statuses.put("CONFIRMED", 2L);
        DashboardStatisticsResponse response = new DashboardStatisticsResponse(
                range.from(), range.to(), "UTC", 3, 2, 1, 1, statuses);

        statuses.clear();

        assertThat(response.appointmentsByStatus().keySet()).containsExactly("PENDING", "CONFIRMED");
        assertThat(response.appointmentsByStatus().values()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> response.appointmentsByStatus().put("PENDING", 99L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
