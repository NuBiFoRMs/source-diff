package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DirectoryService {

    private final FileRepository fileRepository;

    private final ModelMapper modelMapper;

    public List<FileResponse> getRepositories() {
        return fileRepository.findAllByParentIdIsNull()
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    public FileResponse getParent(String path) {
        Long parentId = fileRepository.findByFilePathAndFileType(path, FileType.DIRECTORY)
                .map(FileEntity::getParentId)
                .orElse(null);

        if (Objects.nonNull(parentId))
            return fileRepository.findById(parentId)
                    .map(this::map)
                    .orElse(null);

        return null;
    }

    public List<FileResponse> getParentPath(String path) {
        List<FileEntity> fileList = new ArrayList<>();

        FileEntity fileEntity = fileRepository.findByFilePath(path)
                .orElseThrow(RuntimeException::new);
        fileList.add(fileEntity);

        while (Objects.nonNull(fileEntity.getParentId())) {
            fileEntity = fileRepository.findById(fileEntity.getParentId())
                    .orElseThrow(RuntimeException::new);
            fileList.add(fileEntity);
        }

        return fileList
                .stream()
                .sorted(Comparator.comparing(FileEntity::getId))
                .map(this::map)
                .collect(Collectors.toList());
    }

    public List<FileResponse> getFileList(String path) {
        Long directoryId = fileRepository.findByFilePathAndFileType(path, FileType.DIRECTORY)
                .orElseThrow(RuntimeException::new)
                .getId();

        return fileRepository.findAllByParentId(directoryId, Sort.by("fileType", "filePath"))
                .stream()
                .map(this::map)
                .map(fileResponse -> {
                    if (FileType.DIRECTORY.equals(fileResponse.getFileType()))
                        return compressPath(fileResponse);
                    else
                        return fileResponse;
                })
                .collect(Collectors.toList());
    }

    private FileResponse compressPath(FileResponse fileResponse) {
        String fileName = fileResponse.getFileName();

        List<FileEntity> fileList = fileRepository.findAllByParentId(fileResponse.getId());
        Map<String, Long> count = fileList
                .stream()
                .collect(Collectors.groupingBy(FileEntity::getFileType, Collectors.counting()));

        while (Objects.nonNull(count.get(FileType.DIRECTORY)) && count.get(FileType.DIRECTORY) == 1 &&
                Objects.isNull(count.get(FileType.FILE))) {
            FileEntity fileEntity = fileList.get(0);
            fileResponse = map(fileEntity);
            fileResponse.setFileName(fileName + PathUtils.SEPARATOR + fileResponse.getFileName());
            fileName = fileResponse.getFileName();

            fileList = fileRepository.findAllByParentId(fileEntity.getId());
            count = fileList
                    .stream()
                    .collect(Collectors.groupingBy(FileEntity::getFileType, Collectors.counting()));
        }

        return fileResponse;
    }

    private FileResponse map(FileEntity fileEntity) {
        FileResponse fileResponse = modelMapper.map(fileEntity, FileResponse.class);
        fileResponse.setFilePathDisplay(fileResponse.getFilePath());
        return fileResponse;
    }

    public List<FileResponse> getFilterFileList(String path) {
        return fileRepository.findAllByFilePathStartsWith(path, Sort.by("filePath", "fileType"))
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }
}
