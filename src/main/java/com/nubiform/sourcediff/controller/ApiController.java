package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.mail.MailMessage;
import com.nubiform.sourcediff.service.BatchService;
import com.nubiform.sourcediff.service.MailService;
import com.nubiform.sourcediff.vo.ApiResponse;
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
    public static final String REPOSITORY_PATH = "/{repository}";

    private final AppProperties appProperties;

    private final MailService mailService;
    private final BatchService batchService;

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

    @ResponseBody
    @PostMapping(MAIL_URI)
    public ResponseEntity<ApiResponse<Object>> mail(@RequestBody MailMessage mailMessage) {
        log.info("request: {}, to: {}, subject: {}", MAIL_URI, mailMessage.getTo(), mailMessage.getSubject());
        log.debug("message\n{}", mailMessage.getMessage());

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @ResponseBody
    @GetMapping("/scan")
    public ResponseEntity<ApiResponse<Object>> batchScan() {
        log.info("request: {}", "/scan");

        batchService.scan();

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @ResponseBody
    @GetMapping("/svn-info-scan")
    public ResponseEntity<ApiResponse<Object>> batchSvnInfoScan() {
        log.info("request: {}", "/svn-info-scan");

        batchService.svnInfoScan();

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }

    @ResponseBody
    @GetMapping("/svn-info")
    public ResponseEntity<ApiResponse<Object>> batchSvnInfo() {
        log.info("request: {}", "/scan");

        batchService.svnInfo();

        return ResponseEntity
                .ok(ApiResponse.builder()
                        .message("success")
                        .build());
    }
}
