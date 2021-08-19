package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Slf4j
@RequiredArgsConstructor
@Service
public class BatchService {

    private final AppProperties appProperties;

    private final ScanService scanService;

    private final FileRepository fileRepository;

    @Scheduled(fixedDelay = 150000)
    public void scan() {
        appProperties.getRepositories()
                .parallelStream()
                .forEach(scanService::scan);
    }

    @Scheduled(fixedDelay = 5000)
    public void svnInfo() {
        fileRepository.findAll()
                .parallelStream()
                .filter(FileEntity::needToUpdateSvnInfo)
                .sorted(Comparator.comparing(FileEntity::getDevModified).reversed())
                .limit(20)
                .forEach(scanService::updateSvnInfo);
    }
}
