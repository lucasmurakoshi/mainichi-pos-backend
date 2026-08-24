package com.mainichi.pos.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mainichi.pos.dto.VentaRequestDTO;
import com.mainichi.pos.dto.VentaResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;

@Repository
public class VentaRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public VentaRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public VentaResponseDTO ejecutarSpVenta(VentaRequestDTO request) {
        try {
            String detallesJson = objectMapper.writeValueAsString(request.items());
            String pagosJson = objectMapper.writeValueAsString(request.pagos());

            return jdbcTemplate.execute((java.sql.Connection conn) -> {
                try (CallableStatement cs = conn.prepareCall("{CALL sp_registrar_venta(?, ?, ?, ?, ?, ?, ?)}")) {
                    if (request.turnoId() != null) {
                        cs.setInt(1, request.turnoId());
                    } else {
                        cs.setNull(1, Types.INTEGER);
                    }
                    cs.setBigDecimal(2, request.montoAbonado());
                    cs.setBigDecimal(3, request.vuelto());
                    cs.setString(4, detallesJson);
                    cs.setString(5, pagosJson);
                    cs.registerOutParameter(6, Types.INTEGER);
                    cs.registerOutParameter(7, Types.VARCHAR);

                    cs.execute();

                    String codigoError = cs.getString(7);
                    if (codigoError != null && !"OK".equalsIgnoreCase(codigoError)) {
                        throw new IllegalStateException(codigoError);
                    }

                    int ventaId = cs.getInt(6);
                    return new VentaResponseDTO(ventaId, "Venta registrada con éxito");
                }
            });
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar la venta: " + e.getMessage(), e);
        }
    }
}