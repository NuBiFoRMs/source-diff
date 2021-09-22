package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.service.DirectoryService;
import com.nubiform.sourcediff.service.MailService;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ExplorerController {

    public static final String HOME_URI = "/";
    public static final String EXPLORER_URI = "/explorer";
    public static final String FILTER_URI = "/filter";
    public static final String MAILING_URI = "/mailing";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String ANT_PATTERN = "/**";
    public static final String MAILING_TEST_URI = "/mailing-test";

    private final AppProperties appProperties;

    private final DirectoryService directoryService;
    private final MailService mailService;

    private final FileRepository fileRepository;

    private final ModelMapper modelMapper;

    @GetMapping(HOME_URI)
    public String home(Model model) {
        log.info("request: {}", HOME_URI);

        model.addAttribute("repositories", appProperties.getRepositories());
        model.addAttribute("files", directoryService.getRepositories());

        return "home";
    }

    @GetMapping(EXPLORER_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String explorer(@PathVariable String repository, Model model, HttpServletRequest request) {
        String path = PathUtils.removePrefix(request.getRequestURI(), EXPLORER_URI);
        log.info("request: {}, repository: {}, path: {}", EXPLORER_URI, repository, path);

        model.addAttribute("repositories", appProperties.getRepositories());
        model.addAttribute("path", path);
        model.addAttribute("parentPath", directoryService.getParentPath(path));
        model.addAttribute("parent", directoryService.getParent(path));
        model.addAttribute("files", directoryService.getFileList(path));

        return "explorer";
    }

    @GetMapping(FILTER_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String filter(@PathVariable String repository,
                         @RequestParam(required = false) String search,
                         @RequestParam(required = false) boolean diff,
                         @RequestParam(required = false) boolean dev,
                         @RequestParam(required = false) boolean prod,
                         Model model, HttpServletRequest request) {
        String path = PathUtils.removePrefix(request.getRequestURI(), FILTER_URI);
        log.info("request: {}, repository: {}, path: {}, search: {}, diff: {}, dev: {}, prod: {}", FILTER_URI, repository, path, search, diff, dev, prod);

        model.addAttribute("repositories", appProperties.getRepositories());
        model.addAttribute("path", path);
        model.addAttribute("parentPath", directoryService.getParentPath(path));
        model.addAttribute("parent", directoryService.getParent(path));
        model.addAttribute("diffCheck", diff);
        model.addAttribute("devCheck", dev);
        model.addAttribute("prodCheck", prod);

        List<FileResponse> fileList = directoryService.getFilterFileList(path);

        if (StringUtils.isNotBlank(search)) {
            model.addAttribute("search", search);
            fileList = fileList
                    .stream()
                    .filter(fileResponse -> StringUtils.containsIgnoreCase(fileResponse.getFilePath(), search))
                    .map(fileResponse -> {
                        fileResponse.setFilePathDisplay(PathUtils.highlight(fileResponse.getFilePath(), search, s -> "<span class=\"highlight\">" + s + "</span>"));
                        return fileResponse;
                    })
                    .collect(Collectors.toList());
        }

        if (diff || dev || prod) {
            fileList = fileList
                    .stream()
                    .filter(fileResponse -> (diff && fileResponse.canDiff()) || (dev && fileResponse.isDevOnly()) || (prod && fileResponse.isProdOnly()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("files", fileList);

        return "filter";
    }
}
