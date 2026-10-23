package com.mediturno.backend.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mediturno.backend.statistics.exception.InvalidStatisticsQueryException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StatisticsGroupingTest {

    @ParameterizedTest
    @CsvSource({"day, DAY", "DAY, DAY", "dAy, DAY", "status, STATUS",
            "STATUS, STATUS", "specialty, SPECIALTY", "SPECIALTY, SPECIALTY"})
    void acceptsSupportedGroupingWithoutDependingOnCase(String parameter, StatisticsGrouping expected) {
        assertThat(StatisticsGrouping.parse(parameter)).isEqualTo(expected);
        assertThat(StatisticsGrouping.parse(parameter).parameter()).isEqualTo(expected.parameter());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "month", "doctor", "statuses", " day", "day ", "day; DROP TABLE citas"})
    void rejectsUnsupportedGroupingWithPublicQueryError(String parameter) {
        assertThatThrownBy(() -> StatisticsGrouping.parse(parameter))
                .isInstanceOf(InvalidStatisticsQueryException.class)
                .hasMessageContaining("day, status o specialty");
    }
}
