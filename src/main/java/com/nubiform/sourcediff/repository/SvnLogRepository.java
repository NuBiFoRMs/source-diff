package com.nubiform.sourcediff.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SvnLogRepository extends JpaRepository<SvnLogEntity, Long> {
}
