package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class BatchService {

    private final AppProperties appProperties;

    private final ScanService scanService;

    private final FileRepository fileRepository;

    @Scheduled(fixedDelay = 150000)
    public void scan() {
        log.info("start batch: scan");
        appProperties.getRepositories()
                .parallelStream()
                .forEach(scanService::scan);
        log.info("finish batch: scan");
    }

    @Scheduled(fixedDelay = 5000)
    public void svnInfo() {
        log.info("start batch: svnInfo");
        fileRepository.findAllForUpdateSvnInfo()
                .stream()
                .limit(20)
                .collect(Collectors.toList())
                .parallelStream()
                .forEach(scanService::updateSvnInfo);
        log.info("finish batch: svnInfo");
    }
}
