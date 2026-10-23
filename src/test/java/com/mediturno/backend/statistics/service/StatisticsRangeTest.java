package com.mediturno.backend.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mediturno.backend.statistics.exception.InvalidStatisticsQueryException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class StatisticsRangeTest {

    private static final Clock LIMA_CLOCK = Clock.fixed(
            Instant.parse("2026-10-23T02:30:00Z"), ZoneId.of("America/Lima"));

    @Test
    void includesBothEndsOfThirtyOneDayMonthUsingExclusiveSqlUpperBound() {
        StatisticsRange range = new StatisticsRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(range.startAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(range.endBefore()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
        assertThat(ChronoUnit.DAYS.between(range.startAt(), range.endBefore())).isEqualTo(31);
    }

    @Test
    void singleDayRangeCoversExactlyOneDay() {
        LocalDate date = LocalDate.of(2024, 2, 29);
        StatisticsRange range = new StatisticsRange(date, date);

        assertThat(range.startAt()).isEqualTo(date.atStartOfDay());
        assertThat(range.endBefore()).isEqualTo(LocalDateTime.of(2024, 3, 1, 0, 0));
    }

    @Test
    void acceptsEntireLeapYearAtMaximumInclusiveLength() {
        StatisticsRange range = new StatisticsRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(ChronoUnit.DAYS.between(range.startAt(), range.endBefore())).isEqualTo(366);
        assertThat(range.endBefore()).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0));
    }

    @Test
    void rejectsThreeHundredSixtySevenInclusiveDays() {
        assertThatThrownBy(() -> new StatisticsRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)))
                .isInstanceOf(InvalidStatisticsQueryException.class)
                .hasMessageContaining("366");
    }

    @Test
    void rejectsReversedRangeBeforeItReachesSql() {
        assertThatThrownBy(() -> new StatisticsRange(
                LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 22)))
                .isInstanceOf(InvalidStatisticsQueryException.class)
                .hasMessageContaining("from");
    }

    @Test
    void rejectsIncompleteDirectConstruction() {
        LocalDate date = LocalDate.of(2026, 10, 23);

        assertThatThrownBy(() -> new StatisticsRange(null, date))
                .isInstanceOf(InvalidStatisticsQueryException.class);
        assertThatThrownBy(() -> new StatisticsRange(date, null))
                .isInstanceOf(InvalidStatisticsQueryException.class);
    }

    @Test
    void rejectsDatesOutsideMysqlRangeAndExtremeJavaDates() {
        assertThatThrownBy(() -> new StatisticsRange(LocalDate.MIN, LocalDate.MIN))
                .isInstanceOf(InvalidStatisticsQueryException.class);
        assertThatThrownBy(() -> new StatisticsRange(LocalDate.MAX, LocalDate.MAX))
                .isInstanceOf(InvalidStatisticsQueryException.class);
        assertThatThrownBy(() -> new StatisticsRange(
                LocalDate.of(999, 12, 31), LocalDate.of(1000, 1, 1)))
                .isInstanceOf(InvalidStatisticsQueryException.class);
        assertThatThrownBy(() -> new StatisticsRange(
                LocalDate.of(9999, 12, 30), LocalDate.of(9999, 12, 31)))
                .isInstanceOf(InvalidStatisticsQueryException.class);
    }

    @Test
    void acceptedDateLimitsLeaveRoomForExclusiveUpperBound() {
        StatisticsRange earliest = new StatisticsRange(
                LocalDate.of(1000, 1, 1), LocalDate.of(1000, 1, 1));
        StatisticsRange latest = new StatisticsRange(
                LocalDate.of(9999, 12, 30), LocalDate.of(9999, 12, 30));

        assertThat(earliest.startAt()).isEqualTo(LocalDateTime.of(1000, 1, 1, 0, 0));
        assertThat(latest.endBefore()).isEqualTo(LocalDateTime.of(9999, 12, 31, 0, 0));
    }

    @Test
    void defaultThirtyDaysUseUtcDateEvenWhenClockHasAnotherZone() {
        assertThat(LocalDate.now(LIMA_CLOCK)).isEqualTo(LocalDate.of(2026, 10, 22));

        StatisticsRange range = StatisticsRange.resolve(null, null, LIMA_CLOCK);

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(ChronoUnit.DAYS.between(range.startAt(), range.endBefore())).isEqualTo(30);
    }

    @Test
    void suppliedEndDateAnchorsDefaultThirtyDayWindow() {
        StatisticsRange range = StatisticsRange.resolve(null, LocalDate.of(2024, 3, 1), LIMA_CLOCK);

        assertThat(range.from()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(ChronoUnit.DAYS.between(range.startAt(), range.endBefore())).isEqualTo(30);
    }

    @Test
    void suppliedStartDateUsesUtcTodayForMissingEndDate() {
        StatisticsRange range = StatisticsRange.resolve(LocalDate.of(2026, 10, 1), null, LIMA_CLOCK);

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 10, 23));
    }

    @Test
    void rejectsExtremeEndDatesBeforeDefaultWindowCanOverflow() {
        assertThatThrownBy(() -> StatisticsRange.resolve(null, LocalDate.MIN, LIMA_CLOCK))
                .isInstanceOf(InvalidStatisticsQueryException.class);
        assertThatThrownBy(() -> StatisticsRange.resolve(null, LocalDate.MAX, LIMA_CLOCK))
                .isInstanceOf(InvalidStatisticsQueryException.class);
    }
}
