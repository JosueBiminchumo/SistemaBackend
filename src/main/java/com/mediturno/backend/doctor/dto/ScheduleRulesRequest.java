package com.mediturno.backend.doctor.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ScheduleRulesRequest(@NotNull @Valid List<ScheduleRuleRequest> rules) {
}
