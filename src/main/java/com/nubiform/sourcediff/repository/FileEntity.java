package com.nubiform.sourcediff.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

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

    public boolean needToScan() {
        return Objects.isNull(this.scanModified) ||
                (Objects.nonNull(this.devFilePath) && this.scanModified.isBefore(this.devModified)) ||
                (Objects.nonNull(this.prodFilePath) && this.scanModified.isBefore(this.prodModified));
    }

    public boolean needToUpdateSvnInfo() {
        return Objects.isNull(this.infoModified) ||
                (Objects.nonNull(this.devFilePath) && this.infoModified.isBefore(this.devModified)) ||
                (Objects.nonNull(this.prodFilePath) && this.infoModified.isBefore(this.prodModified));
    }
}
