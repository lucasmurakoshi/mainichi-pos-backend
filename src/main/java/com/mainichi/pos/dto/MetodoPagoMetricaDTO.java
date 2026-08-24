package com.mainichi.pos.dto;

import java.math.BigDecimal;

public record MetodoPagoMetricaDTO(
    String metodoPago,
    BigDecimal totalFacturado,
    Long cantidadTransacciones
) {}
