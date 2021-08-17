package com.nubiform.sourcediff.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findAllByParentIdIsNull();

    Optional<FileEntity> findByFilePath(String filePath);

    Optional<FileEntity> findByFilePathAndFileType(String filePath, String fileType);

    List<FileEntity> findAllByParentId(Long parentId);

    List<FileEntity> findAllByParentId(Long parentId, Sort sort);

    List<FileEntity> findAllByFilePathStartsWith(String filePath);

    List<FileEntity> findAllByFilePathStartsWith(String filePath, Sort sort);
}
