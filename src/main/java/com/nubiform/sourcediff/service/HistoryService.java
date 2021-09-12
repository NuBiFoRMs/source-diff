package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.repository.SvnLogRepository;
import com.nubiform.sourcediff.svn.SvnConnector;
import com.nubiform.sourcediff.svn.SvnException;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.SvnInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class HistoryService {

    private final AppProperties appProperties;

    private final SvnConnector svnConnector;

    private final FileRepository fileRepository;
    private final SvnLogRepository svnLogRepository;

    private final ModelMapper modelMapper;

    public List<String> exportFile(String path, SourceType sourceType, String revision) throws IOException {
        FileEntity fileEntity = fileRepository.findByFilePathAndFileType(path, FileType.FILE)
                .orElseThrow(RuntimeException::new);

        path = PathUtils.removePrefix(fileEntity.getFilePath(), fileEntity.getRepository());

        File location = new File(fileEntity.getRepository() + PathUtils.SEPARATOR + sourceType + "_" + revision + path);
        if (location.exists()) {
            return FileUtils.readLines(location, StandardCharsets.UTF_8);
        }

        if (!location.getParentFile().exists()) {
            log.info("mkdir: {}", location.getParentFile().getPath());
            if (!location.getParentFile().mkdirs())
                throw new RemoteException();
        }

        AppProperties.RepositoryProperties repository = appProperties.getRepository(fileEntity.getRepository());

        String url = repository.getUrl(sourceType);
        String username = repository.getUsername(sourceType);
        String password = repository.getPassword(sourceType);

        try {
            svnConnector.export(url + path, revision, location.getParentFile(), username, password);
        } catch (SvnException e) {
            return null;
        }

        return FileUtils.readLines(location, StandardCharsets.UTF_8);
    }

    public List<SvnInfoResponse> getRevisionList(String path, SourceType sourceType) {
        List<SvnInfoResponse> collect = svnLogRepository.findAllRevision(path, sourceType.toString())
                .stream()
                .limit(20)
                .map(svnLog -> modelMapper.map(svnLog, SvnInfoResponse.class))
                .collect(Collectors.toList());
        return collect;
    }

    public Long getLastRevision(String path, SourceType sourceType) {
        return fileRepository.findByFilePath(path)
                .filter(fileEntity -> Objects.nonNull(fileEntity.getFilePath(sourceType)))
                .map(fileEntity -> fileEntity.getRevision(sourceType))
                .orElse(-1L);
    }
}
