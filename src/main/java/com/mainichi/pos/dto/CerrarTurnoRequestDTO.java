package com.mainichi.pos.dto;

import java.math.BigDecimal;

public record CerrarTurnoRequestDTO(
    Integer turnoId,
    BigDecimal montoEfectivoReal,
    BigDecimal montoMpReal,
    BigDecimal montoTarjetaReal,
    String observaciones
) {}
