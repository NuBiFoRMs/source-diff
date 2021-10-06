package com.nubiform.sourcediff.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SvnLogRepository extends JpaRepository<SvnLogEntity, Long> {

    @Query("select max(l.revision) from SvnLogEntity l where l.repository = :repository and l.sourceType = :sourceType")
    Optional<Long> findLastRevisionByRepositoryAndSourceType(String repository, String sourceType);

    @Query("select l from SvnLogEntity l where " +
//            "l.revision >= (select max(l2.revision) from SvnLogEntity l2 where l2.filePath = :filePath and l2.sourceType = :sourceType and l2.action = 'A') and " +
//            "l.revision > coalesce((select max(l2.revision) from SvnLogEntity l2 where l2.filePath = :filePath and l2.sourceType = :sourceType and l2.action = 'D'), 0) and " +
            "l.filePath = :filePath and l.sourceType = :sourceType order by l.revision desc")
    List<SvnLogEntity> findAllRevision(String filePath, String sourceType);

    List<SvnLogEntity> findAllByRepositoryAndSourceType(String repository, String sourceType);
}
