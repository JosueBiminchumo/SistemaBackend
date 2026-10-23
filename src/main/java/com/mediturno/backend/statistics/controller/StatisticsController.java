package com.mediturno.backend.statistics.controller;

import com.mediturno.backend.common.dto.ApiResponse;
import com.mediturno.backend.statistics.dto.DashboardStatisticsResponse;
import com.mediturno.backend.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "Estadisticas", description = "Indicadores administrativos. Requiere rol ADMIN.")
public class StatisticsController {

    private final StatisticsService service;

    public StatisticsController(StatisticsService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Consultar el resumen del dashboard",
            description = "Citas por fecha de atencion UTC; pacientes, medicos y especialidades "
                    + "son totales globales. Rango inclusivo de hasta 366 dias; por defecto 30 dias.")
    public ApiResponse<DashboardStatisticsResponse> dashboard(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.dashboard(from, to));
    }
}
