package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.repository.SvnLogRepository;
import com.nubiform.sourcediff.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@RequiredArgsConstructor
@Controller
public class RevisionController {

    public static final String REVISION_URI = "/revision";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String SOURCE_TYPE_PATH = "/{sourceType}";
    public static final String REVISION_PATH = "/{revision}";
    public static final String ANT_PATTERN = "/**";

    private final AppProperties appProperties;

    private final HistoryService historyService;

    private final SvnLogRepository svnLogRepository;

    @GetMapping(REVISION_URI + REPOSITORY_PATH + SOURCE_TYPE_PATH)
    public String revision(@PathVariable String repository, @PathVariable SourceType sourceType, Model model) {
        log.info("request: {}, repository: {}, sourceType: {}", REVISION_URI, repository, sourceType);

        model.addAttribute("repositories", appProperties.getRepositories());
        model.addAttribute("svnLogs", historyService.getLogList(repository, sourceType));

        return "revision";
    }

    @GetMapping(REVISION_URI + REPOSITORY_PATH + SOURCE_TYPE_PATH + REVISION_PATH + ANT_PATTERN)
    public String revision(@PathVariable String repository,
                           @PathVariable SourceType sourceType,
                           @PathVariable Long revision,
                           Model model) {
        log.info("request: {}, repository: {}, sourceType: {}, revision: {}", REVISION_URI, repository, sourceType, revision);

        model.addAttribute("repositories", appProperties.getRepositories());
        model.addAttribute("svnLogFiles", historyService.getLogFileList(repository, sourceType, revision));

        return "revision-file";
    }
}
