package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.DiffType;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.mail.MailMessage;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.service.DiffService;
import com.nubiform.sourcediff.service.DirectoryService;
import com.nubiform.sourcediff.service.HistoryService;
import com.nubiform.sourcediff.service.MailService;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.DiffResponse;
import com.nubiform.sourcediff.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final DiffService diffService;
    private final MailService mailService;
    private final HistoryService historyService;

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

    @GetMapping(VIEW_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String view(@PathVariable String repository,
                       @RequestParam(required = false) String dev,
                       @RequestParam(required = false) String prod,
                       Model model, HttpServletRequest request) throws IOException {
        String path = PathUtils.removePrefix(request.getRequestURI(), VIEW_URI);
        log.info("request: {}, repository: {}, path: {}, dev: {}, prod: {}", VIEW_URI, repository, path, dev, prod);

        model.addAttribute("path", path);
        model.addAttribute("parentPath", directoryService.getParentPath(path));
        model.addAttribute("devRevision", historyService.getRevisionList(path, SourceType.DEV));
        model.addAttribute("prodRevision", historyService.getRevisionList(path, SourceType.PROD));
        model.addAttribute("selectedDev", dev);
        model.addAttribute("selectedProd", prod);

        List<DiffResponse> diffResponseList = diffService.getDiff(path, dev, prod);

        if (diffResponseList.stream()
                .anyMatch(diffResponse -> !DiffType.EQUAL.equals(diffResponse.getChangeType()) && !DiffType.SKIP.equals(diffResponse.getChangeType()))) {
            diffResponseList = diffService.setDiffView(diffResponseList);
            model.addAttribute("diffList", diffService.getDiffList(diffResponseList));
            model.addAttribute("diff", diffResponseList);
            return "diff-view";
        } else {
            model.addAttribute("diff", diffResponseList);
            return "view";
        }
    }

    @GetMapping("/mailing")
    public String mailing() {
        log.info("request: {}", "/mailing");

        appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .forEach(mailService::mailing);

        return "mail-success";
    }

    @ResponseBody
    @PostMapping("/mail")
    public String mail(@RequestBody MailMessage mailMessage) {
        log.info("request: {}, to: {}, subject: {}", "/mail", mailMessage.getTo(), mailMessage.getSubject());
        log.debug("message\n{}", mailMessage.getMessage());
        return "SUCCESS";
    }

    @GetMapping("/mailing-test/{repository}")
    public String mailingTest(@PathVariable String repository, Model model) {
        List<FileResponse> files = fileRepository.findAllByFilePathStartsWith(PathUtils.SEPARATOR + repository, Sort.by("filePath", "fileType"))
                .stream()
                .filter(file -> file.getDiffCount() > 0 || Objects.isNull(file.getDevFilePath()) || Objects.isNull(file.getProdFilePath()))
                .filter(file -> FileType.FILE.equals(file.getFileType()))
                .map(fileEntity -> {
                    FileResponse fileResponse = modelMapper.map(fileEntity, FileResponse.class);
                    fileResponse.setFilePathDisplay(fileResponse.getFilePath());
                    return fileResponse;
                })
                .collect(Collectors.toList());

        model.addAttribute("host", appProperties.getHost());
        model.addAttribute("files", files);

        return "mail";
    }

    @GetMapping("/sample")
    public String sample() {
        return "sample";
    }
}
