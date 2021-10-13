package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.mail.MailMessage;
import com.nubiform.sourcediff.service.MailService;
import com.nubiform.sourcediff.service.ScanService;
import com.nubiform.sourcediff.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ApiController {

    public static final String MAILING_URI = "/mailing";
    public static final String MAIL_URI = "/mail";
    public static final String SCAN_URI = "/scan";
    public static final String SVN_INFO_SCAN_URI = "/svn-info-scan";
    public static final String REPOSITORY_PATH = "/{repository}";

    private final AppProperties appProperties;

    private final MailService mailService;
    private final ScanService scanService;

    @GetMapping(MAILING_URI)
    public ResponseEntity<ApiResponse<Object>> mailing() {
        log.info("request: {}", MAILING_URI);

        appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .forEach(mailService::mailing);

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @GetMapping(value = MAILING_URI + REPOSITORY_PATH)
    public ResponseEntity<ApiResponse<Object>> mailing(@PathVariable String repository) {
        log.info("request: {}, repository: {}", MAILING_URI, repository);

        appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .filter(repo -> repo.equals(repository))
                .forEach(mailService::mailing);

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @Operation(hidden = true)
    @PostMapping(MAIL_URI)
    public ResponseEntity<ApiResponse<Object>> mail(@RequestBody MailMessage mailMessage) {
        log.info("request: {}, to: {}, subject: {}", MAIL_URI, mailMessage.getTo(), mailMessage.getSubject());
        log.debug("message\n{}", mailMessage.getMessage());

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @GetMapping(SCAN_URI)
    public ResponseEntity<ApiResponse<Object>> batchScan() {
        log.info("request: {}", SCAN_URI);

        appProperties.getRepositories()
                .parallelStream()
                .forEach(scanService::scan);

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @GetMapping(SCAN_URI + REPOSITORY_PATH)
    public ResponseEntity<ApiResponse<Object>> batchScan(@PathVariable String repository) {
        log.info("request: {}", SCAN_URI);

        appProperties.getRepositories()
                .stream()
                .filter(repo -> repo.getName().equals(repository))
                .findFirst()
                .ifPresent(scanService::scan);

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @Operation(hidden = true)
    @GetMapping(SVN_INFO_SCAN_URI)
    public ResponseEntity<ApiResponse<Object>> batchSvnInfoScan() {
        log.info("request: {}", SVN_INFO_SCAN_URI);

        appProperties.getRepositories()
                .parallelStream()
                .forEach(scanService::scanSvnInfo);

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }
}
