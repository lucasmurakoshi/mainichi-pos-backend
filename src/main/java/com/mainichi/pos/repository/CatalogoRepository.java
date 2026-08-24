package com.mainichi.pos.repository;

import com.mainichi.pos.dto.ProductoDTO;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CatalogoRepository {

    private final JdbcClient jdbcClient;

    public CatalogoRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ProductoDTO> listarProductosActivos() {
        String sql = """
            SELECT 
                p.id, 
                p.categoria_id AS categoriaId, 
                c.nombre AS categoriaNombre, 
                p.nombre, 
                p.precio_unitario AS precioUnitario, 
                p.es_frecuente AS esFrecuente
            FROM productos p
            INNER JOIN categorias c ON p.categoria_id = c.id
            WHERE p.activo = 1
            ORDER BY p.es_frecuente DESC, p.nombre ASC
        """;

        return jdbcClient.sql(sql)
                .query(ProductoDTO.class)
                .list();
    }
}