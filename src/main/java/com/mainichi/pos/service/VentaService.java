package com.mainichi.pos.service;

import com.mainichi.pos.dto.VentaDetalleDTO;
import com.mainichi.pos.dto.VentaRequestDTO;
import com.mainichi.pos.dto.VentaResponseDTO;
import com.mainichi.pos.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;

    public VentaService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @Transactional
    public VentaResponseDTO registrarVenta(VentaRequestDTO request) {
        if (request == null) {
            throw new IllegalStateException("La solicitud de venta no puede ser nula");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalStateException("La venta debe contener al menos un producto");
        }

        // Calcular total esperado de los ítems de forma null-safe
        BigDecimal totalCalculado = BigDecimal.ZERO;
        for (VentaDetalleDTO item : request.items()) {
            if (item != null && item.subtotal() != null) {
                totalCalculado = totalCalculado.add(item.subtotal());
            }
        }

        // Validar monto abonado suficiente
        if (request.montoAbonado() != null && request.montoAbonado().compareTo(totalCalculado) < 0) {
            throw new IllegalStateException("Monto abonado insuficiente: el total es " + totalCalculado + " y se abonó " + request.montoAbonado());
        }

        // Ejecutar registro mediante el repositorio (SP con rollback y actualización de stock)
        return ventaRepository.ejecutarSpVenta(request);
    }
}
