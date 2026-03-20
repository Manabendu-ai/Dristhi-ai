package com.drishti.verify.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handle(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error",   ex.getMessage()
        ));
    }
}
