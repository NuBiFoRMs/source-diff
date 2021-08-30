package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.DiffType;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.constant.SourceType;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
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
    public static final String MAILING_URI = "/mailing";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String ANT_PATTERN = "/**";
    public static final String MAILING_TEST_URI = "/mailing-test";

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
                       @RequestParam(required = false) Long dev,
                       @RequestParam(required = false) Long prod,
                       Model model, HttpServletRequest request,
                       RedirectAttributes redirectAttributes) throws IOException {
        String path = PathUtils.removePrefix(request.getRequestURI(), VIEW_URI);
        log.info("request: {}, repository: {}, path: {}, dev: {}, prod: {}", VIEW_URI, repository, path, dev, prod);

        if (Objects.isNull(dev) || Objects.isNull(prod)) {
            redirectAttributes.addAttribute("dev", historyService.getLastRevision(path, SourceType.DEV));
            redirectAttributes.addAttribute("prod", historyService.getLastRevision(path, SourceType.PROD));
            return "redirect:" + VIEW_URI + path;
        }

        model.addAttribute("path", path);
        model.addAttribute("parentPath", directoryService.getParentPath(path));
        model.addAttribute("dev", dev);
        model.addAttribute("prod", prod);

        List<SourceType> sourceTypes = new ArrayList<>();
        sourceTypes.add(SourceType.DEV);
        sourceTypes.add(SourceType.PROD);
        sourceTypes
                .stream()
                .forEach(sourceType -> {
                    model.addAttribute(sourceType + "Revision", historyService.getRevisionList(path, sourceType));
                });
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

    @GetMapping(value = MAILING_URI, produces = MediaType.TEXT_HTML_VALUE)
    public String mailing() {
        log.info("request: {}", MAILING_URI);

        appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .forEach(mailService::mailing);

        return "mail-success";
    }

    @GetMapping(value = MAILING_URI + REPOSITORY_PATH, produces = MediaType.TEXT_HTML_VALUE)
    public String mailing(@PathVariable String repository) {
        log.info("request: {}, repository: {}", MAILING_URI, repository);

        appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .filter(repo -> repo.equals(repository))
                .forEach(mailService::mailing);

        return "mail-success";
    }

    @GetMapping(MAILING_TEST_URI + REPOSITORY_PATH)
    public String mailingTest(@PathVariable String repository, Model model) {
        log.info("request: {}, repository: {}", MAILING_TEST_URI, repository);

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
}
