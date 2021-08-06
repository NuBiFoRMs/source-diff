package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.util.SvnUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class ScanService {

    private final AppProperties appProperties;

    @Scheduled(fixedDelay = 180000)
    public void scan() {
        appProperties.getRepositories()
                .forEach(this::scan);
    }

    public void scan(AppProperties.RepositoryProperties repositoryProperties) {
        log.info("start scan: {}", repositoryProperties.getName());

        File devPath = new File(repositoryProperties.getName() + "/" + SourceType.DEV);
        File prodPath = new File(repositoryProperties.getName() + "/" + SourceType.PROD);

        log.info("svn checkout");
        SvnUtils.checkout(repositoryProperties.getDevUrl(), "HEAD", devPath, repositoryProperties.getSvnUsername(), repositoryProperties.getSvnPassword());
        SvnUtils.checkout(repositoryProperties.getProdUrl(), "HEAD", prodPath, repositoryProperties.getSvnUsername(), repositoryProperties.getSvnPassword());

        log.info("finish scan: {}", repositoryProperties.getName());
    }
}

