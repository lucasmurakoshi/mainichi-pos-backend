package com.mainichi.pos.dto;

import java.time.LocalDateTime;

public record AbrirTurnoResponseDTO(
    Integer turnoId,
    String mensaje,
    LocalDateTime fechaApertura
) {}
