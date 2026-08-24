package com.mainichi.pos.service;

import com.mainichi.pos.dto.VentaDetalleDTO;
import com.mainichi.pos.dto.VentaPagoDTO;
import com.mainichi.pos.dto.VentaRequestDTO;
import com.mainichi.pos.dto.VentaResponseDTO;
import com.mainichi.pos.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private VentaService ventaService;

    private VentaRequestDTO ventaValidaRequest;

    @BeforeEach
    void setUp() {
        List<VentaDetalleDTO> items = List.of(
                new VentaDetalleDTO(1, 2, new BigDecimal("2500.00"), new BigDecimal("5000.00")),
                new VentaDetalleDTO(2, 1, new BigDecimal("3800.00"), new BigDecimal("3800.00"))
        );

        List<VentaPagoDTO> pagos = List.of(
                new VentaPagoDTO("EFECTIVO", new BigDecimal("10000.00"), "")
        );

        ventaValidaRequest = new VentaRequestDTO(
                1,
                new BigDecimal("10000.00"),
                new BigDecimal("1200.00"),
                items,
                pagos
        );
    }

    @Test
    @DisplayName("Debe registrar venta exitosamente cuando los datos son válidos")
    void testRegistrarVenta_Exito() {
        VentaResponseDTO responseEsperada = new VentaResponseDTO(101, "Venta registrada con éxito");
        when(ventaRepository.ejecutarSpVenta(any(VentaRequestDTO.class))).thenReturn(responseEsperada);

        VentaResponseDTO resultado = ventaService.registrarVenta(ventaValidaRequest);

        assertNotNull(resultado);
        assertEquals(101, resultado.ventaId());
        assertEquals("Venta registrada con éxito", resultado.mensaje());
        verify(ventaRepository, times(1)).ejecutarSpVenta(ventaValidaRequest);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el monto abonado es insuficiente para cubrir los ítems")
    void testRegistrarVenta_ErrorMontoInsuficiente() {
        // Total esperado = 5000 + 3800 = 8800. Se abona 5000 (insuficiente)
        VentaRequestDTO requestInsuficiente = new VentaRequestDTO(
                1,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                ventaValidaRequest.items(),
                ventaValidaRequest.pagos()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ventaService.registrarVenta(requestInsuficiente)
        );

        assertTrue(exception.getMessage().contains("Monto abonado insuficiente"));
        verify(ventaRepository, never()).ejecutarSpVenta(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la lista de ítems está vacía")
    void testRegistrarVenta_ErrorItemsVacios() {
        VentaRequestDTO requestSinItems = new VentaRequestDTO(
                1,
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                Collections.emptyList(),
                ventaValidaRequest.pagos()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ventaService.registrarVenta(requestSinItems)
        );

        assertEquals("La venta debe contener al menos un producto", exception.getMessage());
        verify(ventaRepository, never()).ejecutarSpVenta(any());
    }

    @Test
    @DisplayName("Debe propagar error de regla de negocio cuando el turno se encuentra cerrado")
    void testRegistrarVenta_ErrorTurnoCerrado() {
        when(ventaRepository.ejecutarSpVenta(any(VentaRequestDTO.class)))
                .thenThrow(new IllegalStateException("No se puede registrar la venta: el turno se encuentra cerrado"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ventaService.registrarVenta(ventaValidaRequest)
        );

        assertTrue(exception.getMessage().contains("turno se encuentra cerrado"));
        verify(ventaRepository, times(1)).ejecutarSpVenta(ventaValidaRequest);
    }

    @Test
    @DisplayName("Debe propagar excepción y provocar rollback transaccional ante fallos en la base de datos")
    void testRegistrarVenta_ErrorRollbackTransaccional() {
        when(ventaRepository.ejecutarSpVenta(any(VentaRequestDTO.class)))
                .thenThrow(new RuntimeException("Error fatal en conexión de base de datos"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> ventaService.registrarVenta(ventaValidaRequest)
        );

        assertTrue(exception.getMessage().contains("Error fatal"));
        verify(ventaRepository, times(1)).ejecutarSpVenta(ventaValidaRequest);
    }
}
