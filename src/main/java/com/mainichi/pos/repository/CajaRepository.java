package com.mainichi.pos.repository;

import com.mainichi.pos.dto.AbrirTurnoRequestDTO;
import com.mainichi.pos.dto.AbrirTurnoResponseDTO;
import com.mainichi.pos.dto.CerrarTurnoRequestDTO;
import com.mainichi.pos.dto.CerrarTurnoResponseDTO;
import com.mainichi.pos.dto.EstadoTurnoDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

@Repository
public class CajaRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;

    public CajaRepository(JdbcTemplate jdbcTemplate, JdbcClient jdbcClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcClient = jdbcClient;
    }

    public AbrirTurnoResponseDTO abrirTurno(AbrirTurnoRequestDTO request) {
        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{CALL sp_abrir_turno(?, ?, ?, ?, ?)}")) {
                cs.setInt(1, request.usuarioId());
                cs.setBigDecimal(2, request.montoInicial() != null ? request.montoInicial() : BigDecimal.ZERO);
                cs.setString(3, request.observaciones());
                cs.registerOutParameter(4, Types.INTEGER); // OUT p_turno_id
                cs.registerOutParameter(5, Types.VARCHAR); // OUT p_codigo_error

                cs.execute();

                String codigoError = cs.getString(5);
                if (codigoError != null && !"OK".equalsIgnoreCase(codigoError)) {
                    throw new IllegalStateException(codigoError);
                }

                int turnoId = cs.getInt(4);
                return new AbrirTurnoResponseDTO(turnoId, "Turno abierto exitosamente", LocalDateTime.now());
            }
        });
    }

    public CerrarTurnoResponseDTO cerrarTurno(CerrarTurnoRequestDTO request) {
        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            try (CallableStatement cs = conn.prepareCall("{CALL sp_cerrar_turno(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                cs.setInt(1, request.turnoId());
                cs.setBigDecimal(2, request.montoEfectivoReal() != null ? request.montoEfectivoReal() : BigDecimal.ZERO);
                cs.setBigDecimal(3, request.montoMpReal() != null ? request.montoMpReal() : BigDecimal.ZERO);
                cs.setBigDecimal(4, request.montoTarjetaReal() != null ? request.montoTarjetaReal() : BigDecimal.ZERO);
                cs.setString(5, request.observaciones());
                cs.registerOutParameter(6, Types.DECIMAL); // OUT p_total_esperado
                cs.registerOutParameter(7, Types.DECIMAL); // OUT p_total_real
                cs.registerOutParameter(8, Types.DECIMAL); // OUT p_diferencia
                cs.registerOutParameter(9, Types.VARCHAR); // OUT p_codigo_error

                cs.execute();

                String codigoError = cs.getString(9);
                if (codigoError != null && !"OK".equalsIgnoreCase(codigoError)) {
                    throw new IllegalStateException(codigoError);
                }

                BigDecimal totalEsperado = cs.getBigDecimal(6);
                BigDecimal totalReal = cs.getBigDecimal(7);
                BigDecimal diferencia = cs.getBigDecimal(8);

                return new CerrarTurnoResponseDTO(
                        request.turnoId(),
                        totalEsperado,
                        totalReal,
                        diferencia,
                        "Turno cerrado y arqueo registrado exitosamente",
                        LocalDateTime.now()
                );
            }
        });
    }

    public EstadoTurnoDTO obtenerEstadoTurno(Integer usuarioId) {
        String sql = """
            SELECT id, usuario_id AS usuarioId, usuario_nombre AS usuarioNombre, 
                   monto_inicial AS montoInicial, fecha_apertura AS fechaApertura, estado
            FROM turnos 
            WHERE usuario_id = :usuarioId AND estado = 'ABIERTO'
            ORDER BY fecha_apertura DESC 
            LIMIT 1
        """;

        return jdbcClient.sql(sql)
                .param("usuarioId", usuarioId)
                .query((rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("fechaApertura");
                    LocalDateTime fechaApertura = ts != null ? ts.toLocalDateTime() : null;
                    return new EstadoTurnoDTO(
                            true,
                            rs.getInt("id"),
                            rs.getInt("usuarioId"),
                            rs.getString("usuarioNombre"),
                            rs.getBigDecimal("montoInicial"),
                            fechaApertura,
                            rs.getString("estado")
                    );
                })
                .optional()
                .orElse(new EstadoTurnoDTO(
                        false,
                        null,
                        usuarioId,
                        null,
                        BigDecimal.ZERO,
                        null,
                        "CERRADO"
                ));
    }
}
