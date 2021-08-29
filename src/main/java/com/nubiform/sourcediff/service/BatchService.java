package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BatchService {

    private final AppProperties appProperties;

    private final ScanService scanService;
    private final MailService mailService;

    private final FileRepository fileRepository;

    @Scheduled(fixedDelayString = "${scheduler.scan}")
    public void scan() {
        log.info("start batch: scan");
        appProperties.getRepositories()
                .parallelStream()
                .forEach(scanService::scan);
        log.info("finish batch: scan");
    }

    public void svnInfoScan() {
        log.info("start batch: svnInfoScan");
        appProperties.getRepositories()
                .parallelStream()
                .forEach(scanService::scanSvnInfo);
        log.info("finish batch: svnInfoScan");
    }

    @Scheduled(cron = "${scheduler.mailing}")
    public void mailing() {
        appProperties.getRepositories()
                .stream()
                .map(AppProperties.RepositoryProperties::getName)
                .forEach(mailService::mailing);
    }
}
