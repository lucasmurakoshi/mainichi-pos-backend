package com.mainichi.pos.repository;

import com.mainichi.pos.dto.MetodoPagoMetricaDTO;
import com.mainichi.pos.dto.MetricasResumenDTO;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Repository
public class MetricasRepository {

    private final JdbcClient jdbcClient;

    public MetricasRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public MetricasResumenDTO obtenerResumenJornada(Integer turnoId) {
        String sqlVentas = """
            SELECT 
                IFNULL(SUM(v.total), 0.00) AS totalFacturado,
                COUNT(v.id) AS cantidadVentas
            FROM ventas v
            WHERE (:turnoId IS NULL OR v.turno_id = :turnoId)
        """;

        var resumenVentas = jdbcClient.sql(sqlVentas)
                .param("turnoId", turnoId)
                .query((rs, rowNum) -> new Object[]{
                        rs.getBigDecimal("totalFacturado"),
                        rs.getLong("cantidadVentas")
                })
                .single();

        BigDecimal totalFacturado = (BigDecimal) resumenVentas[0];
        Long cantidadVentas = (Long) resumenVentas[1];

        BigDecimal ticketPromedio = BigDecimal.ZERO;
        if (cantidadVentas != null && cantidadVentas > 0 && totalFacturado != null) {
            ticketPromedio = totalFacturado.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP);
        }

        String sqlPagos = """
            SELECT 
                vp.metodo_pago AS metodoPago,
                IFNULL(SUM(vp.monto), 0.00) AS totalFacturado,
                COUNT(vp.id) AS cantidadTransacciones
            FROM venta_pagos vp
            INNER JOIN ventas v ON vp.venta_id = v.id
            WHERE (:turnoId IS NULL OR v.turno_id = :turnoId)
            GROUP BY vp.metodo_pago
            ORDER BY totalFacturado DESC
        """;

        List<MetodoPagoMetricaDTO> desglose = jdbcClient.sql(sqlPagos)
                .param("turnoId", turnoId)
                .query(MetodoPagoMetricaDTO.class)
                .list();

        return new MetricasResumenDTO(
                totalFacturado != null ? totalFacturado : BigDecimal.ZERO,
                cantidadVentas != null ? cantidadVentas : 0L,
                ticketPromedio,
                desglose
        );
    }
}
