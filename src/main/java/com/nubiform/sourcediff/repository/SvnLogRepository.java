package com.nubiform.sourcediff.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SvnLogRepository extends JpaRepository<SvnLogEntity, Long> {

    @Query("select max(l.revision) from SvnLogEntity l where l.repository = :repository and l.sourceType = :sourceType")
    Optional<Long> findLastRevisionByRepositoryAndSourceType(String repository, String sourceType);

    List<SvnLogEntity> findAllByFilePathAndSourceTypeOrderByRevisionDesc(String filePath, String sourceType);
}
