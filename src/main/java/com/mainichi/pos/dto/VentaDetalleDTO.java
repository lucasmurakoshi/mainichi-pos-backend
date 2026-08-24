package com.mainichi.pos.dto;

import java.math.BigDecimal;

public record VentaDetalleDTO(
    Integer productoId,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {}
