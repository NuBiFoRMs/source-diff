package com.nubiform.sourcediff.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
public class ErrorHandlingController implements ErrorController {

    @RequestMapping("/error")
    public String errorHandling(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
        log.error("errorHandling: {}", httpStatus);
        return "error";
    }
}
