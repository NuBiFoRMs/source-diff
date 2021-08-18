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
import com.nubiform.sourcediff.svn.SvnConnector;
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

    private final FilenameFilter filenameFilter;

    @Transactional
    public void scan(AppProperties.RepositoryProperties repositoryProperties) {
        log.info("start scan: {}", repositoryProperties.getName());

        File devPath = new File(repositoryProperties.getName() + PathUtils.SEPARATOR + SourceType.DEV);
        File prodPath = new File(repositoryProperties.getName() + PathUtils.SEPARATOR + SourceType.PROD);

        log.debug("svn checkout");
        svnConnector.checkout(repositoryProperties.getDevUrl(), "HEAD", devPath, repositoryProperties.getDevUsername(), repositoryProperties.getDevPassword());
        svnConnector.checkout(repositoryProperties.getProdUrl(), "HEAD", prodPath, repositoryProperties.getProdUsername(), repositoryProperties.getProdPassword());

        log.debug("clean cache");
        fileRepository.cleanByRepository(repositoryProperties.getName());

        log.debug("scan directory");
        directoryScan(repositoryProperties.getName(), SourceType.DEV, devPath);
        directoryScan(repositoryProperties.getName(), SourceType.PROD, prodPath);

        log.debug("clean deleted file");
        fileRepository.deleteByRepository(repositoryProperties.getName());

        log.debug("scan init diff");
        detailScan(PathUtils.SEPARATOR + repositoryProperties.getName());

        log.info("finish scan: {}", repositoryProperties.getName());
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

        if (SourceType.DEV.equals(sourceType)) {
            fileEntity.setDevFilePath(path);
            fileEntity.setDevModified(lastModified);
        } else {
            fileEntity.setProdFilePath(path);
            fileEntity.setProdModified(lastModified);
        }

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
                if (Objects.isNull(file.getScanModified()) ||
                        (Objects.nonNull(file.getDevFilePath()) && file.getScanModified().isBefore(file.getDevModified())) ||
                        (Objects.nonNull(file.getProdFilePath()) && file.getScanModified().isBefore(file.getProdModified()))) {

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

    @Transactional
    public FileEntity updateSvnInfo(FileEntity file) {
        if (Objects.isNull(file.getInfoModified()) ||
                (Objects.nonNull(file.getDevFilePath()) && file.getInfoModified().isBefore(file.getDevModified())) ||
                (Objects.nonNull(file.getProdFilePath()) && file.getInfoModified().isBefore(file.getProdModified()))) {

            // svn info.
            if (Objects.nonNull(file.getDevFilePath())) {
                Map<String, Object> svnInfo = svnConnector.log(new File(file.getDevFilePath()));
                log.debug("svnInfo: {}", svnInfo);
                file.setDevRevision((String) svnInfo.get("revision"));
                file.setDevMessage((String) svnInfo.get("msg"));
                file.setDevCommitTime((LocalDateTime) svnInfo.get("date"));
                file.setDevAuthor((String) svnInfo.get("author"));
            }
            if (Objects.nonNull(file.getProdFilePath())) {
                Map<String, Object> svnInfo = svnConnector.log(new File(file.getProdFilePath()));
                log.debug("svnInfo: {}", svnInfo);
                file.setProdRevision((String) svnInfo.get("revision"));
                file.setProdMessage((String) svnInfo.get("msg"));
                file.setProdCommitTime((LocalDateTime) svnInfo.get("date"));
                file.setProdAuthor((String) svnInfo.get("author"));
            }

            file.setInfoModified(LocalDateTime.now());
        }

        return fileRepository.save(file);
    }
}
