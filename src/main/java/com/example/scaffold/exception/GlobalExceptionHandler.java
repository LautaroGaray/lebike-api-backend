package com.example.scaffold.exception;

import com.example.scaffold.dto.ResponseData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResponseData> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ResponseData(null, false, ex.getMessage()));
    }
}

