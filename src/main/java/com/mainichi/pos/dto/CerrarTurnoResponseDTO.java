package com.mainichi.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CerrarTurnoResponseDTO(
    Integer turnoId,
    BigDecimal totalEsperado,
    BigDecimal totalReal,
    BigDecimal diferencia,
    String mensaje,
    LocalDateTime fechaCierre
) {}
