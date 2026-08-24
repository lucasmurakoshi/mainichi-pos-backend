package com.mainichi.pos.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Regla de Negocio",
            "message", ex.getMessage() != null ? ex.getMessage() : "Error en regla de negocio"
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.NOT_FOUND.value(),
            "error", "No Encontrado",
            "message", ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado"
        ));
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        if (ex instanceof ErrorResponseException) {
            throw (ErrorResponseException) ex;
        }
        String detailedMessage = ex.getMessage();
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            detailedMessage = (detailedMessage != null ? detailedMessage + " -> " : "") + ex.getCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error", "Error Interno",
            "message", detailedMessage != null ? detailedMessage : "Error interno del servidor"
        ));
    }
}
