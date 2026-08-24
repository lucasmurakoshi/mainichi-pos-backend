package com.mainichi.pos.controller;

import com.mainichi.pos.dto.MetricasResumenDTO;
import com.mainichi.pos.repository.MetricasRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metricas")
@Tag(name = "Dashboard y Métricas", description = "Endpoints para métricas de facturación, desglose de pagos y ticket promedio")
public class MetricasController {

    private final MetricasRepository metricasRepository;

    public MetricasController(MetricasRepository metricasRepository) {
        this.metricasRepository = metricasRepository;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Obtener resumen de métricas", description = "Devuelve totales facturados agrupados por método de pago y ticket promedio de la jornada")
    public ResponseEntity<MetricasResumenDTO> obtenerResumen(@RequestParam(required = false) Integer turnoId) {
        MetricasResumenDTO resumen = metricasRepository.obtenerResumenJornada(turnoId);
        return ResponseEntity.ok(resumen);
    }
}
