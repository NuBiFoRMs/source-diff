package com.nubiform.sourcediff.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RevisionController {

    public static final String REVISION_URI = "/revision";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String SOURCE_TYPE_PATH = "/{sourceType}";
    public static final String ANT_PATTERN = "/**";

    @GetMapping(REVISION_URI + REPOSITORY_PATH + SOURCE_TYPE_PATH + ANT_PATTERN)
    public String revision() {
        return "revision";
    }
}
