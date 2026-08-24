package com.mainichi.pos.dto;

import java.math.BigDecimal;

public record AbrirTurnoRequestDTO(
    Integer usuarioId,
    BigDecimal montoInicial,
    String observaciones
) {}
