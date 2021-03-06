package com.nubiform.sourcediff.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class ErrorControllerAdvisor {

    @ExceptionHandler(Exception.class)
    public String exception(Exception e) {
        log.error("exception: {}, {}", e.getClass(), e.getLocalizedMessage());
        return "error";
    }
}
