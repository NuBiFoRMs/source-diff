package com.nubiform.sourcediff.repository;

import com.nubiform.sourcediff.constant.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "svn_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"repository", "filePath"})
        })
public class SvnLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repository;

    private SourceType sourceType;

    private String filePath;

    private String fileType;

    private String revision;

    @Column(length = 500)
    private String message;

    private LocalDateTime commitTime;

    private String author;
}
