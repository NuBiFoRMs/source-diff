package com.nubiform.sourcediff.repository;

import com.nubiform.sourcediff.constant.SourceType;
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

    private Long devRevision;

    @Column(length = 500)
    private String devMessage;

    private LocalDateTime devCommitTime;

    private String devAuthor;

    private String prodFilePath;

    private LocalDateTime prodModified;

    private Long prodRevision;

    @Column(length = 500)
    private String prodMessage;

    private LocalDateTime prodCommitTime;

    private String prodAuthor;

    private Integer diffCount;

    private LocalDateTime scanModified;

    public boolean needToScan() {
        return Objects.isNull(this.scanModified) ||
                (Objects.nonNull(this.devFilePath) && this.scanModified.isBefore(this.devModified)) ||
                (Objects.nonNull(this.prodFilePath) && this.scanModified.isBefore(this.prodModified));
    }

    public String getFilePath(SourceType sourceType) {
        return SourceType.DEV.equals(sourceType) ? devFilePath : prodFilePath;
    }

    public Long getRevision(SourceType sourceType) {
        return SourceType.DEV.equals(sourceType) ? devRevision : prodRevision;
    }

    public void setFilePath(SourceType sourceType, String filePath) {
        if (SourceType.DEV.equals(sourceType))
            this.devFilePath = filePath;
        else
            this.prodFilePath = filePath;
    }

    public void setModified(SourceType sourceType, LocalDateTime modified) {
        if (SourceType.DEV.equals(sourceType))
            this.devModified = modified;
        else
            this.prodModified = modified;
    }

    public void setRevision(SourceType sourceType, Long revision) {
        if (SourceType.DEV.equals(sourceType))
            this.devRevision = revision;
        else
            this.prodRevision = revision;
    }

    public void setMessage(SourceType sourceType, String message) {
        if (SourceType.DEV.equals(sourceType))
            this.devMessage = message;
        else
            this.prodMessage = message;
    }

    public void setCommitTime(SourceType sourceType, LocalDateTime commitTime) {
        if (SourceType.DEV.equals(sourceType))
            this.devCommitTime = commitTime;
        else
            this.prodCommitTime = commitTime;
    }

    public void setAuthor(SourceType sourceType, String author) {
        if (SourceType.DEV.equals(sourceType))
            this.devAuthor = author;
        else
            this.prodAuthor = author;
    }
}
