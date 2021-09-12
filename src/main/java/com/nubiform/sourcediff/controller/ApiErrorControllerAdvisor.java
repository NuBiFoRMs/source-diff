package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ApiErrorControllerAdvisor {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> exception(Exception e) {
        log.error("exception: {}, {}", e.getClass(), e.getLocalizedMessage());
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.builder()
                        .message("error")
                        .data(e.getLocalizedMessage())
                        .build());
    }
}
