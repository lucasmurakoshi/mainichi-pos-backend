package com.mainichi.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record MetricasResumenDTO(
    BigDecimal totalFacturado,
    Long cantidadVentas,
    BigDecimal ticketPromedio,
    List<MetodoPagoMetricaDTO> desgloseMetodosPago
) {}
