package com.mainichi.pos.dto;

import java.math.BigDecimal;

public record VentaPagoDTO(
    String metodoPago,
    BigDecimal monto,
    String referencia
) {}