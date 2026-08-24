package com.mainichi.pos.controller;

import com.mainichi.pos.dto.AbrirTurnoRequestDTO;
import com.mainichi.pos.dto.AbrirTurnoResponseDTO;
import com.mainichi.pos.dto.CerrarTurnoRequestDTO;
import com.mainichi.pos.dto.CerrarTurnoResponseDTO;
import com.mainichi.pos.dto.EstadoTurnoDTO;
import com.mainichi.pos.repository.CajaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/caja")
@Tag(name = "Caja y Turnos", description = "Endpoints para apertura de turno, arqueo ciego y verificación de terminal")
public class CajaController {

    private final CajaRepository cajaRepository;

    public CajaController(CajaRepository cajaRepository) {
        this.cajaRepository = cajaRepository;
    }

    @PostMapping("/abrir")
    @Operation(summary = "Abrir turno de caja", description = "Inicia un nuevo turno con fondo inicial de caja")
    public ResponseEntity<AbrirTurnoResponseDTO> abrirTurno(@RequestBody AbrirTurnoRequestDTO request) {
        AbrirTurnoResponseDTO response = cajaRepository.abrirTurno(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/cerrar")
    @Operation(summary = "Cerrar turno y registrar arqueo ciego", description = "Cierra el turno actual calculando totales esperados y diferencias")
    public ResponseEntity<CerrarTurnoResponseDTO> cerrarTurno(@RequestBody CerrarTurnoRequestDTO request) {
        CerrarTurnoResponseDTO response = cajaRepository.cerrarTurno(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado/{usuarioId}")
    @Operation(summary = "Consultar estado de la terminal", description = "Valida si el usuario actual tiene un turno abierto y habilitado para cobrar")
    public ResponseEntity<EstadoTurnoDTO> obtenerEstadoTerminal(@PathVariable Integer usuarioId) {
        EstadoTurnoDTO estado = cajaRepository.obtenerEstadoTurno(usuarioId);
        return ResponseEntity.ok(estado);
    }
}
