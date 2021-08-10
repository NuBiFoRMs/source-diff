package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.service.DirectoryService;
import com.nubiform.sourcediff.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
@Controller
public class DiffController {

    public static final String HOME_URI = "/";
    public static final String EXPLORER_URI = "/explorer";
    public static final String FILTER_URI = "/filter";
    public static final String VIEW_URI = "/view";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String ANT_PATTERN = "/**";

    private final AppProperties appProperties;

    private final DirectoryService directoryService;

    @GetMapping(HOME_URI)
    public String home(Model model) {
        log.info("request: {}", HOME_URI);

        model.addAttribute("repositories", appProperties.getRepositories());

        return "home";
    }

    @GetMapping(EXPLORER_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String explorer(@PathVariable String repository, Model model, HttpServletRequest request) {
        String path = PathUtils.removePrefix(request.getRequestURI(), EXPLORER_URI);
        log.info("request: {}, repository: {}, path: {}", EXPLORER_URI, repository, path);

        return "explorer";
    }

    @GetMapping(FILTER_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String filter(@PathVariable String repository, Model model, HttpServletRequest request) {
        String path = PathUtils.removePrefix(request.getRequestURI(), FILTER_URI);
        log.info("request: {}, repository: {}, path: {}", FILTER_URI, repository, path);

        return "filter";
    }

    @GetMapping(VIEW_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String view(@PathVariable String repository, Model model, HttpServletRequest request) {
        String path = PathUtils.removePrefix(request.getRequestURI(), VIEW_URI);
        log.info("request: {}, repository: {}, path: {}", VIEW_URI, repository, path);

        return "view";
    }
}
