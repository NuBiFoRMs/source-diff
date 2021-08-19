package com.nubiform.sourcediff.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    @Modifying
    @Query("select f from FileEntity f where f.infoModified is null or (f.devModified is not null and f.infoModified < f.devModified) or (f.prodModified is not null and f.infoModified < f.prodModified) order by f.devModified desc")
    List<FileEntity> findAllForUpdateSvnInfo();

    @Modifying
    @Query("update FileEntity f set f.devFilePath = null, f.devModified = null, f.prodFilePath = null, f.prodModified = null where f.repository = :repository")
    void cleanByRepository(String repository);

    @Modifying
    @Query("delete from FileEntity f where f.repository = :repository and f.devFilePath is null and f.prodFilePath is null")
    void deleteByRepository(String repository);
}
