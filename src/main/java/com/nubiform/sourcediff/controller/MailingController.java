package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.service.MailService;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
public class MailingController {

    public static final String MAILING_URI = "/mailing";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String MAILING_TEST_URI = "/mailing-test";

    private final AppProperties appProperties;

    private final MailService mailService;

    private final FileRepository fileRepository;

    private final ModelMapper modelMapper;

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
