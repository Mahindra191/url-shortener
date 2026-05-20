package com.dev.project.url_shortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.dev.project.url_shortener.exception.AliasConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;
import java.util.HashMap;



@ControllerAdvice
public class GlobalException {

    // 1. SPECIFIC: Handles validation/format errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "400");
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 2. SPECIFIC: Create a "UrlNotFoundException" (highly recommended) 
    // If you haven't created it yet, you can catch the specific error 
    // thrown by .orElseThrow() in your service.
    @ExceptionHandler(UrlNotFoundException.class) 
    public ResponseEntity<Map<String, String>> handleNotFound(UrlNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "404");
        error.put("error", "Not Found");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 3. CATCH-ALL: For everything else (Actual 500 errors)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalError(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "500");
        error.put("error", "Internal Server Error");
        error.put("message", "Something went wrong on our end.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    

    @ExceptionHandler(AliasConflictException.class)
    public ResponseEntity<Map<String, String>> handleAliasConflict(AliasConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", ex.getMessage(),
            "fallbackUrl", ex.getExistingShortUrl()
        ));
    }
}