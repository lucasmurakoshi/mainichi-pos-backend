package com.mainichi.pos.dto;

import java.math.BigDecimal;

public record ProductoDTO(
    Integer id,
    Integer categoriaId,
    String categoriaNombre,
    String nombre,
    BigDecimal precioUnitario,
    boolean esFrecuente
) {}
