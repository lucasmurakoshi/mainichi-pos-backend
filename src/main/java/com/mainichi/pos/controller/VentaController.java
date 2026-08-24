package com.mainichi.pos.controller;

import com.mainichi.pos.dto.VentaRequestDTO;
import com.mainichi.pos.dto.VentaResponseDTO;
import com.mainichi.pos.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas y Cobros", description = "Endpoints para registro transaccional de ventas y control de pagos")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    @Operation(summary = "Registrar venta", description = "Registra una nueva venta, descuenta stock y guarda el detalle de los pagos")
    public ResponseEntity<VentaResponseDTO> registrarVenta(@RequestBody VentaRequestDTO request) {
        VentaResponseDTO response = ventaService.registrarVenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}