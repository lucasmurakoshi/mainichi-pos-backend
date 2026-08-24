package com.mainichi.pos.controller;

import com.mainichi.pos.dto.ProductoDTO;
import com.mainichi.pos.repository.CatalogoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@Tag(name = "Catálogo de Productos", description = "Endpoints para consulta de productos activos para la botonera del POS")
public class CatalogoController {

    private final CatalogoRepository catalogoRepository;

    public CatalogoController(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    @GetMapping("/productos")
    @Operation(summary = "Listar productos activos", description = "Devuelve el catálogo de productos ordenados por favoritos/frecuentes y nombre")
    public ResponseEntity<List<ProductoDTO>> listarProductos() {
        return ResponseEntity.ok(catalogoRepository.listarProductosActivos());
    }
}