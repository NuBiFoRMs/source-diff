package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
public class ErrorHandlingController implements ErrorController {

    public static final String ERROR = "/error";

    @RequestMapping(value = ERROR, produces = MediaType.TEXT_HTML_VALUE)
    public String errorHandlingHtml(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
        log.error("errorHandlingHtml: {}", httpStatus);
        return "error";
    }

    @RequestMapping(value = ERROR)
    public ResponseEntity<ApiResponse<Object>> errorHandling(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
        log.error("errorHandling: {}", httpStatus);
        return ResponseEntity
                .status(httpStatus.value())
                .body(ApiResponse.builder()
                        .code(httpStatus.value())
                        .message("error")
                        .data(httpStatus.getReasonPhrase())
                        .build());
    }
}
