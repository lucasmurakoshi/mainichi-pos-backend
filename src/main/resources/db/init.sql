-- Limpieza preventiva
DROP PROCEDURE IF EXISTS sp_cerrar_turno;
DROP PROCEDURE IF EXISTS sp_abrir_turno;
DROP PROCEDURE IF EXISTS sp_registrar_venta;
DROP TABLE IF EXISTS venta_pagos;
DROP TABLE IF EXISTS venta_detalles;
DROP TABLE IF EXISTS ventas;
DROP TABLE IF EXISTS turnos;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS categorias;

-- Tabla de Categorías
CREATE TABLE IF NOT EXISTS categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Productos
CREATE TABLE IF NOT EXISTS productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    categoria_id INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    es_frecuente BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- Tabla de Turnos / Cajas (Soporte Arqueo Ciego)
CREATE TABLE IF NOT EXISTS turnos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL DEFAULT 1,
    usuario_nombre VARCHAR(100) DEFAULT 'Cajero',
    monto_inicial DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    monto_efectivo_real DECIMAL(10, 2) NULL,
    monto_mp_real DECIMAL(10, 2) NULL,
    monto_tarjeta_real DECIMAL(10, 2) NULL,
    total_esperado DECIMAL(10, 2) NULL,
    total_real DECIMAL(10, 2) NULL,
    diferencia DECIMAL(10, 2) NULL,
    observaciones TEXT NULL,
    fecha_apertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'ABIERTO'
);

-- Tabla de Ventas
CREATE TABLE IF NOT EXISTS ventas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    turno_id INT,
    cliente VARCHAR(150) DEFAULT 'Consumidor Final',
    monto_abonado DECIMAL(10, 2) DEFAULT 0.00,
    vuelto DECIMAL(10, 2) DEFAULT 0.00,
    total DECIMAL(10, 2) NOT NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'COMPLETADA',
    observaciones TEXT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (turno_id) REFERENCES turnos(id)
);

-- Tabla de Detalles de Venta
CREATE TABLE IF NOT EXISTS venta_detalles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    producto_id INT,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE SET NULL
);

-- Tabla de Pagos de Venta
CREATE TABLE IF NOT EXISTS venta_pagos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    monto DECIMAL(10, 2) NOT NULL,
    referencia VARCHAR(100),
    FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE
);

-- Datos iniciales de Categorías
INSERT INTO categorias (id, nombre, activo) VALUES
(1, 'Bebidas Calientes', true),
(2, 'Bebidas Frías', true),
(3, 'Comida', true),
(4, 'Postres', true)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre);

-- Datos iniciales de Productos
INSERT INTO productos (id, categoria_id, nombre, descripcion, precio_unitario, stock, es_frecuente, activo) VALUES
(1, 1, 'Café Americano', 'Café espresso diluido con agua caliente', 2500.00, 50, true, true),
(2, 1, 'Matcha Latte', 'Té matcha japonés con leche vaporizada', 3800.00, 35, true, true),
(3, 2, 'Iced Caramel Macchiato', 'Café frío con leche, vainilla y caramelo', 4200.00, 30, true, true),
(4, 3, 'Onigiri Salmón', 'Triángulo de arroz relleno de salmón y alga nori', 2900.00, 20, true, true),
(5, 3, 'Sandwich Tamago Sando', 'Sandwich japonés de huevo cremoso', 3200.00, 15, false, true),
(6, 4, 'Mochi de Frutilla', 'Pastel de arroz tradicional relleno de frutilla', 1800.00, 25, true, true),
(7, 4, 'Dorayaki Anko', 'Pancakes japoneses rellenos de pasta dulce de anko', 2200.00, 20, false, true)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre);

-- Turno inicial por defecto para pruebas
INSERT INTO turnos (id, usuario_id, usuario_nombre, monto_inicial, estado) VALUES
(1, 1, 'Cajero Principal', 10000.00, 'ABIERTO')
ON DUPLICATE KEY UPDATE estado=VALUES(estado);

-- =======================================================
-- SP: sp_abrir_turno
-- =======================================================
DELIMITER //

CREATE PROCEDURE sp_abrir_turno(
    IN p_usuario_id INT,
    IN p_monto_inicial DECIMAL(10, 2),
    IN p_observaciones TEXT,
    OUT p_turno_id INT,
    OUT p_codigo_error VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_error_msg VARCHAR(255);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 v_error_msg = MESSAGE_TEXT;
        ROLLBACK;
        SET p_turno_id = 0;
        SET p_codigo_error = CONCAT('SQL_ERROR: ', v_error_msg);
    END;

    -- Validar si ya existe un turno abierto para este usuario
    SELECT COUNT(*) INTO v_count 
    FROM turnos 
    WHERE usuario_id = p_usuario_id AND estado = 'ABIERTO';

    IF v_count > 0 THEN
        SET p_turno_id = 0;
        SET p_codigo_error = 'El usuario ya tiene un turno abierto activo';
        LEAVE proc_label;
    END IF;

    START TRANSACTION;

    INSERT INTO turnos (usuario_id, usuario_nombre, monto_inicial, observaciones, estado, fecha_apertura)
    VALUES (p_usuario_id, CONCAT('Usuario #', p_usuario_id), IFNULL(p_monto_inicial, 0.00), p_observaciones, 'ABIERTO', NOW());

    SET p_turno_id = LAST_INSERT_ID();
    COMMIT;

    SET p_codigo_error = 'OK';
END //

-- =======================================================
-- SP: sp_cerrar_turno (Arqueo Ciego)
-- =======================================================
CREATE PROCEDURE sp_cerrar_turno(
    IN p_turno_id INT,
    IN p_monto_efectivo_real DECIMAL(10, 2),
    IN p_monto_mp_real DECIMAL(10, 2),
    IN p_monto_tarjeta_real DECIMAL(10, 2),
    IN p_observaciones TEXT,
    OUT p_total_esperado DECIMAL(10, 2),
    OUT p_total_real DECIMAL(10, 2),
    OUT p_diferencia DECIMAL(10, 2),
    OUT p_codigo_error VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_monto_inicial DECIMAL(10, 2) DEFAULT 0.00;
    DECLARE v_total_efectivo_ventas DECIMAL(10, 2) DEFAULT 0.00;
    DECLARE v_total_mp_ventas DECIMAL(10, 2) DEFAULT 0.00;
    DECLARE v_total_tarjeta_ventas DECIMAL(10, 2) DEFAULT 0.00;
    DECLARE v_estado VARCHAR(50);
    DECLARE v_error_msg VARCHAR(255);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 v_error_msg = MESSAGE_TEXT;
        ROLLBACK;
        SET p_total_esperado = 0;
        SET p_total_real = 0;
        SET p_diferencia = 0;
        SET p_codigo_error = CONCAT('SQL_ERROR: ', v_error_msg);
    END;

    -- Verificar existencia y estado del turno
    SELECT estado, monto_inicial 
    INTO v_estado, v_monto_inicial
    FROM turnos 
    WHERE id = p_turno_id;

    IF v_estado IS NULL THEN
        SET p_codigo_error = 'El turno especificado no existe';
        LEAVE proc_label;
    END IF;

    IF v_estado <> 'ABIERTO' THEN
        SET p_codigo_error = 'El turno ya se encuentra cerrado';
        LEAVE proc_label;
    END IF;

    -- Calcular ventas acumuladas por método de pago para este turno
    SELECT IFNULL(SUM(vp.monto), 0.00) INTO v_total_efectivo_ventas
    FROM venta_pagos vp
    INNER JOIN ventas v ON vp.venta_id = v.id
    WHERE v.turno_id = p_turno_id AND UPPER(vp.metodo_pago) = 'EFECTIVO';

    SELECT IFNULL(SUM(vp.monto), 0.00) INTO v_total_mp_ventas
    FROM venta_pagos vp
    INNER JOIN ventas v ON vp.venta_id = v.id
    WHERE v.turno_id = p_turno_id AND (UPPER(vp.metodo_pago) LIKE '%MP%' OR UPPER(vp.metodo_pago) LIKE '%QR%' OR UPPER(vp.metodo_pago) LIKE '%MERCADO%');

    SELECT IFNULL(SUM(vp.monto), 0.00) INTO v_total_tarjeta_ventas
    FROM venta_pagos vp
    INNER JOIN ventas v ON vp.venta_id = v.id
    WHERE v.turno_id = p_turno_id AND (UPPER(vp.metodo_pago) LIKE '%TARJETA%' OR UPPER(vp.metodo_pago) LIKE '%DEBITO%' OR UPPER(vp.metodo_pago) LIKE '%CREDITO%');

    -- Otros pagos que no encajen específicamente se suman al total general de ventas
    -- Total esperado = Monto Inicial (en caja) + Ventas Totales del Turno
    SELECT (v_monto_inicial + IFNULL(SUM(total), 0.00)) INTO p_total_esperado
    FROM ventas 
    WHERE turno_id = p_turno_id;

    IF p_total_esperado IS NULL THEN
        SET p_total_esperado = v_monto_inicial;
    END IF;

    -- Total real ingresado en el arqueo ciego
    SET p_total_real = IFNULL(p_monto_efectivo_real, 0.00) + IFNULL(p_monto_mp_real, 0.00) + IFNULL(p_monto_tarjeta_real, 0.00);

    -- Diferencia = Total Real - Total Esperado
    SET p_diferencia = p_total_real - p_total_esperado;

    START TRANSACTION;

    UPDATE turnos
    SET monto_efectivo_real = p_monto_efectivo_real,
        monto_mp_real = p_monto_mp_real,
        monto_tarjeta_real = p_monto_tarjeta_real,
        total_esperado = p_total_esperado,
        total_real = p_total_real,
        diferencia = p_diferencia,
        observaciones = p_observaciones,
        estado = 'CERRADO',
        fecha_cierre = NOW()
    WHERE id = p_turno_id;

    COMMIT;

    SET p_codigo_error = 'OK';
END //

-- =======================================================
-- SP: sp_registrar_venta
-- =======================================================
CREATE PROCEDURE sp_registrar_venta(
    IN p_turno_id INT,
    IN p_monto_abonado DECIMAL(10, 2),
    IN p_vuelto DECIMAL(10, 2),
    IN p_detalles_json JSON,
    IN p_pagos_json JSON,
    OUT p_venta_id INT,
    OUT p_codigo_error VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_total DECIMAL(10, 2) DEFAULT 0.00;
    DECLARE v_estado_turno VARCHAR(50);
    DECLARE v_error_msg VARCHAR(255);

    -- Manejador de excepciones SQL
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 v_error_msg = MESSAGE_TEXT;
        ROLLBACK;
        SET p_venta_id = 0;
        SET p_codigo_error = CONCAT('SQL_ERROR: ', v_error_msg);
    END;

    -- Validar que el turno exista y esté abierto
    IF p_turno_id IS NOT NULL THEN
        SELECT estado INTO v_estado_turno FROM turnos WHERE id = p_turno_id;
        IF v_estado_turno IS NULL THEN
            SET p_venta_id = 0;
            SET p_codigo_error = 'El turno especificado no existe';
            LEAVE proc_label;
        END IF;
        IF v_estado_turno <> 'ABIERTO' THEN
            SET p_venta_id = 0;
            SET p_codigo_error = 'No se puede registrar la venta: el turno se encuentra cerrado';
            LEAVE proc_label;
        END IF;
    END IF;

    -- Validar que existan ítems en la venta
    IF p_detalles_json IS NULL OR JSON_LENGTH(p_detalles_json) = 0 THEN
        SET p_venta_id = 0;
        SET p_codigo_error = 'La venta debe contener al menos un producto';
        LEAVE proc_label;
    END IF;

    -- Calcular total desde el JSON de ítems
    SELECT IFNULL(SUM(subtotal), 0) INTO v_total
    FROM JSON_TABLE(
        p_detalles_json,
        '$[*]' COLUMNS (
            subtotal DECIMAL(10, 2) PATH '$.subtotal'
        )
    ) AS jt_calc;

    START TRANSACTION;

    -- 1. Insertar Cabecera de Venta
    INSERT INTO ventas (turno_id, monto_abonado, vuelto, total, estado, fecha)
    VALUES (p_turno_id, p_monto_abonado, p_vuelto, v_total, 'COMPLETADA', NOW());

    SET p_venta_id = LAST_INSERT_ID();

    -- 2. Insertar Detalles de la Venta
    INSERT INTO venta_detalles (venta_id, producto_id, cantidad, precio_unitario, subtotal)
    SELECT 
        p_venta_id,
        jt.producto_id,
        jt.cantidad,
        jt.precio_unitario,
        jt.subtotal
    FROM JSON_TABLE(
        p_detalles_json,
        '$[*]' COLUMNS (
            producto_id INT PATH '$.productoId',
            cantidad INT PATH '$.cantidad',
            precio_unitario DECIMAL(10, 2) PATH '$.precioUnitario',
            subtotal DECIMAL(10, 2) PATH '$.subtotal'
        )
    ) AS jt;

    -- 3. Actualizar Stock de Productos
    UPDATE productos p
    INNER JOIN (
        SELECT producto_id, SUM(cantidad) AS cantidad_total
        FROM JSON_TABLE(
            p_detalles_json,
            '$[*]' COLUMNS (
                producto_id INT PATH '$.productoId',
                cantidad INT PATH '$.cantidad'
            )
        ) AS jt_stock
        GROUP BY producto_id
    ) AS items_stock ON p.id = items_stock.producto_id
    SET p.stock = p.stock - items_stock.cantidad_total;

    -- 4. Insertar Pagos de la Venta (si fueron informados)
    IF p_pagos_json IS NOT NULL AND JSON_LENGTH(p_pagos_json) > 0 THEN
        INSERT INTO venta_pagos (venta_id, metodo_pago, monto, referencia)
        SELECT 
            p_venta_id,
            jt_pagos.metodo_pago,
            jt_pagos.monto,
            jt_pagos.referencia
        FROM JSON_TABLE(
            p_pagos_json,
            '$[*]' COLUMNS (
                metodo_pago VARCHAR(50) PATH '$.metodoPago',
                monto DECIMAL(10, 2) PATH '$.monto',
                referencia VARCHAR(100) PATH '$.referencia'
            )
        ) AS jt_pagos;
    END IF;

    COMMIT;
    SET p_codigo_error = 'OK';

END //

DELIMITER ;
