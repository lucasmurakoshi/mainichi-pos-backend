package com.mainichi.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstadoTurnoDTO(
    boolean habilitado,
    Integer turnoId,
    Integer usuarioId,
    String usuarioNombre,
    BigDecimal montoInicial,
    LocalDateTime fechaApertura,
    String estado
) {}
