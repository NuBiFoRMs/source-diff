package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.constant.SourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@RequiredArgsConstructor
@Controller
public class RevisionController {

    public static final String REVISION_URI = "/revision";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String SOURCE_TYPE_PATH = "/{sourceType}";
    public static final String ANT_PATTERN = "/**";

    @GetMapping(REVISION_URI + REPOSITORY_PATH + SOURCE_TYPE_PATH + ANT_PATTERN)
    public String revision(@PathVariable String repository, @PathVariable SourceType sourceType) {
        log.info("request: {}, repository: {}, sourceType: {}", REVISION_URI, repository, sourceType);

        return "revision";
    }
}
