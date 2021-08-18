package com.nubiform.sourcediff.repository;

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
@Table(name = "files",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"repository", "filePath"})
        })
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repository;

    private String filePath;

    private String fileType;

    private String fileName;

    private Long parentId;

    private String devFilePath;

    private LocalDateTime devModified;

    private String devRevision;

    @Column(length = 500)
    private String devMessage;

    private LocalDateTime devCommitTime;

    private String devAuthor;

    private String prodFilePath;

    private LocalDateTime prodModified;

    private String prodRevision;

    @Column(length = 500)
    private String prodMessage;

    private LocalDateTime prodCommitTime;

    private String prodAuthor;

    private Integer diffCount;

    private LocalDateTime scanModified;

    private LocalDateTime infoModified;
}
