package com.mainichi.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record VentaRequestDTO(
    Integer turnoId,
    BigDecimal montoAbonado,
    BigDecimal vuelto,
    List<VentaDetalleDTO> items,
    List<VentaPagoDTO> pagos
) {}