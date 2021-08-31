package com.nubiform.sourcediff.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.repository.SvnLogEntity;
import com.nubiform.sourcediff.repository.SvnLogRepository;
import com.nubiform.sourcediff.svn.SvnConnector;
import com.nubiform.sourcediff.svn.SvnInfo;
import com.nubiform.sourcediff.svn.SvnLog;
import com.nubiform.sourcediff.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScanService {

    private final AppProperties appProperties;

    private final SvnConnector svnConnector;

    private final FileRepository fileRepository;
    private final SvnLogRepository svnLogRepository;

    private final FilenameFilter filenameFilter;

    @Transactional
    public void scan(AppProperties.RepositoryProperties repositoryProperties) {
        log.debug("start scan: {}", repositoryProperties.getName());

        File devPath = new File(repositoryProperties.getName() + PathUtils.SEPARATOR + SourceType.DEV);
        File prodPath = new File(repositoryProperties.getName() + PathUtils.SEPARATOR + SourceType.PROD);

        log.debug("svn checkUpdate");
        long devServerRevision = svnConnector.getHeadRevision(repositoryProperties.getDevUrl(), repositoryProperties.getDevUsername(), repositoryProperties.getDevPassword());
        long prodServerRevision = svnConnector.getHeadRevision(repositoryProperties.getProdUrl(), repositoryProperties.getProdUsername(), repositoryProperties.getProdPassword());

        long devLogRevision = svnLogRepository.findLastRevisionByRepositoryAndSourceType(repositoryProperties.getName(), SourceType.DEV.toString()).orElse(0L);
        long prodLogRevision = svnLogRepository.findLastRevisionByRepositoryAndSourceType(repositoryProperties.getName(), SourceType.PROD.toString()).orElse(0L);

        log.debug("devLogRevision: {}, prodLogRevision: {}", devLogRevision, prodLogRevision);

        boolean devUpdate = checkUpdate(devLogRevision, devServerRevision);
        boolean prodUpdate = checkUpdate(prodLogRevision, prodServerRevision);

        if (devUpdate) {
            log.debug("svn checkout dev");
            svnConnector.checkout(repositoryProperties.getDevUrl(), String.valueOf(devServerRevision), devPath, repositoryProperties.getDevUsername(), repositoryProperties.getDevPassword());
        }

        if (prodUpdate) {
            log.debug("svn checkout prod");
            svnConnector.checkout(repositoryProperties.getProdUrl(), String.valueOf(prodServerRevision), prodPath, repositoryProperties.getProdUsername(), repositoryProperties.getProdPassword());
        }

        if (devUpdate || prodUpdate || !fileRepository.existsByRepository(repositoryProperties.getName())) {
            log.debug("clean cache");
            fileRepository.cleanByRepository(repositoryProperties.getName());

            log.debug("scan directory");
            directoryScan(repositoryProperties.getName(), SourceType.DEV, devPath);
            directoryScan(repositoryProperties.getName(), SourceType.PROD, prodPath);

            log.debug("clean deleted file");
            fileRepository.deleteByRepository(repositoryProperties.getName());

            log.debug("scan init diff");
            detailScan(PathUtils.SEPARATOR + repositoryProperties.getName());
        }

        if (devUpdate) {
            log.debug("svn log dev");
            scanSvnInfo(repositoryProperties.getName(), SourceType.DEV, String.valueOf(devLogRevision == 0 ? 0 : devLogRevision + 1), "BASE", repositoryProperties.getDevUsername(), repositoryProperties.getDevPassword());
        }

        if (prodUpdate) {
            log.debug("svn log prod");
            scanSvnInfo(repositoryProperties.getName(), SourceType.PROD, String.valueOf(prodLogRevision == 0 ? 0 : prodLogRevision + 1), "BASE", repositoryProperties.getProdUsername(), repositoryProperties.getDevPassword());
        }

        log.debug("finish scan: {}", repositoryProperties.getName());
    }

    private boolean checkUpdate(long localRevision, long serverRevision) {
        if (serverRevision == -1)
            throw new RuntimeException("invalid svn server info.");
        return localRevision < serverRevision;
    }

    private void directoryScan(String repository, SourceType sourceType, File baseDirectory) {
        directoryScan(repository, sourceType, baseDirectory.getPath(), baseDirectory, null);
    }

    private void directoryScan(String repository, SourceType sourceType, String rootDirectory, File baseDirectory, Long parentId) {
        log.debug("directoryScan: {}, {}", baseDirectory.getPath(), baseDirectory.getName());
        String path = baseDirectory.getPath();
        String keyPath = PathUtils.removePrefix(path, rootDirectory);
        path = PathUtils.replaceSeparator(path);
        keyPath = PathUtils.SEPARATOR + repository + PathUtils.replaceSeparator(keyPath);

        FileEntity fileEntity = fileRepository.findByFilePath(keyPath)
                .orElse(FileEntity.builder()
                        .repository(repository)
                        .filePath(keyPath)
                        .fileName(parentId == null ? repository : baseDirectory.getName())
                        .parentId(parentId)
                        .diffCount(0)
                        .build());

        LocalDateTime lastModified = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(baseDirectory.lastModified()), TimeZone.getDefault().toZoneId());

        fileEntity.setFilePath(sourceType, path);
        fileEntity.setModified(sourceType, lastModified);

        fileRepository.save(fileEntity);
        log.debug("Id : {}", fileEntity.getId());

        if (baseDirectory.isDirectory()) {
            fileEntity.setFileType(FileType.DIRECTORY);
            Optional.ofNullable(baseDirectory.listFiles(filenameFilter))
                    .map(Arrays::stream)
                    .orElseGet(Stream::empty)
                    .forEach(file -> directoryScan(repository, sourceType, rootDirectory, file, fileEntity.getId()));
        } else {
            fileEntity.setFileType(FileType.FILE);
        }
    }

    private int detailScan(String path) {
        log.debug("diffScan: {}", path);
        FileEntity fileEntity = fileRepository.findByFilePathAndFileType(path, FileType.DIRECTORY)
                .orElseThrow(RuntimeException::new);

        int count = 0;

        List<FileEntity> fileList = fileRepository.findAllByParentId(fileEntity.getId());
        for (FileEntity file : fileList) {
            if (FileType.DIRECTORY.equals(file.getFileType())) {
                count += detailScan(file.getFilePath());
            } else {
                if (file.needToScan()) {
                    if (Objects.nonNull(file.getDevFilePath()) && Objects.nonNull(file.getProdFilePath())) {
                        int diffSize = 0;
                        try {
                            diffSize = getDiffSize(file);
                        } catch (IOException e) {
                            log.error(e.getLocalizedMessage());
                        }
                        file.setDiffCount(diffSize);
                    } else {
                        file.setDiffCount(0);
                    }

                    file.setScanModified(LocalDateTime.now());
                    fileRepository.save(file);
                }
                if (file.getDiffCount() > 0) count++;
            }
        }

        fileEntity.setDiffCount(count);
        fileRepository.save(fileEntity);

        return count;
    }

    private int getDiffSize(FileEntity fileEntity) throws IOException {
        List<String> devSource = FileUtils.readLines(new File(fileEntity.getDevFilePath()), StandardCharsets.UTF_8);
        List<String> prodSource = FileUtils.readLines(new File(fileEntity.getProdFilePath()), StandardCharsets.UTF_8);

        Patch<String> patch = DiffUtils.diff(prodSource, devSource);

        if (patch.getDeltas().size() > 0) {
            log.info("detail diff: {}", fileEntity.getFilePath());
            DiffRowGenerator diffRowGenerator = DiffRowGenerator.create()
                    .inlineDiffByWord(true)
                    .showInlineDiffs(true)
                    .ignoreWhiteSpaces(true)
                    .build();

            return (int) diffRowGenerator.generateDiffRows(prodSource, devSource)
                    .stream()
                    .filter(diffRow -> !DiffRow.Tag.EQUAL.equals(diffRow.getTag()))
                    .count();
        }

        return 0;
    }

    private AppProperties.RepositoryProperties getRepository(String repository) {
        return appProperties.getRepositories()
                .stream()
                .filter(repo -> repo.getName().equals(repository))
                .findFirst()
                .orElseThrow(RuntimeException::new);
    }

    @Transactional
    public void scanSvnInfo(AppProperties.RepositoryProperties repositoryProperties) {
        log.debug("start scanSvnInfo: {}", repositoryProperties.getName());

        svnLogRepository.deleteAll();
        svnLogRepository.flush();

        scanSvnInfo(repositoryProperties.getName(), SourceType.DEV, "0", "BASE", repositoryProperties.getDevUsername(), repositoryProperties.getDevPassword());
        scanSvnInfo(repositoryProperties.getName(), SourceType.PROD, "0", "BASE", repositoryProperties.getProdUsername(), repositoryProperties.getProdPassword());

        log.debug("finish scanSvnInfo: {}", repositoryProperties.getName());
    }

    @Transactional
    public void scanSvnInfo(String repository, SourceType sourceType, String startRevision, String endRevision, String username, String password) {
        log.info("scanSvnInfo: repository: {}, sourceType: {}, startRevision: {}, endRevision: {}", repository, sourceType, startRevision, endRevision);

        File path = new File(repository + PathUtils.SEPARATOR + sourceType);

        SvnInfo svnInfo = svnConnector.svnInfo(path, username, password);
        log.debug("svnInfo: {}", svnInfo);

        String prefix = PathUtils.removePrefix(svnInfo.getUrl(), svnInfo.getRoot());
        log.debug("prefix: {}", prefix);

        List<SvnLog> log = svnConnector.log(path, startRevision, endRevision, username, password);

        log.stream()
                .sorted(Comparator.comparing(SvnLog::getRevision))
                .forEach(svnLog -> saveLog(repository, sourceType, prefix, svnLog));
    }

    private void saveLog(String repository, SourceType sourceType, String prefix, SvnLog svnLog) {
        String repositoryPrefix = PathUtils.SEPARATOR + repository;
        svnLog.getPath()
                .stream()
                .map(path -> SvnLogEntity.builder()
                        .repository(repository)
                        .sourceType(sourceType.toString())
                        .revision(svnLog.getRevision())
                        .filePath(PathUtils.connectPath(repositoryPrefix, PathUtils.removePrefix(path.getFilePath(), prefix)))
                        .fileType(path.getFileType())
                        .action(path.getAction())
                        .message(svnLog.getMessage())
                        .commitTime(svnLog.getDate())
                        .author(svnLog.getAuthor())
                        .build())
                .forEach(svnLogEntity -> {
                    svnLogRepository.save(svnLogEntity);
                    fileRepository.findByFilePath(svnLogEntity.getFilePath())
                            .ifPresent(fileEntity -> {
                                fileEntity.setRevision(sourceType, svnLogEntity.getRevision());
                                fileEntity.setMessage(sourceType, svnLogEntity.getMessage());
                                fileEntity.setCommitTime(sourceType, svnLogEntity.getCommitTime());
                                fileEntity.setAuthor(sourceType, svnLogEntity.getAuthor());
                                fileRepository.save(fileEntity);
                            });
                });
    }
}
