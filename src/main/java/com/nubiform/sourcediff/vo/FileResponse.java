package com.nubiform.sourcediff.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Data
public class FileResponse {

    private Long id;

    private String repository;

    private String filePath;

    private String filePathDisplay;

    private String fileType;

    private String fileName;

    private Long parentId;

    private String devFilePath;

    private String devRevision;

    private String devMessage;

    private LocalDateTime devCommitTime;

    private String devAuthor;

    private String prodFilePath;

    private String prodRevision;

    private String prodMessage;

    private LocalDateTime prodCommitTime;

    private String prodAuthor;

    private Integer diffCount = 0;

    public boolean canDiff() {
        return Objects.nonNull(devFilePath) && Objects.nonNull(prodFilePath) && diffCount > 0;
    }

    public boolean isDevOnly() {
        return Objects.nonNull(devFilePath) && Objects.isNull(prodFilePath);
    }

    public boolean isProdOnly() {
        return Objects.isNull(devFilePath) && Objects.nonNull(prodFilePath);
    }

    public String getNote() {
        if (Objects.isNull(devFilePath))
            return "Prod.";
        else if (Objects.isNull(prodFilePath))
            return "Dev.";
        else
            return null;
    }

    public String getRevision() {
        if (Objects.isNull(devFilePath)) return prodRevision;
        else return devRevision;
    }

    public String getAuthor() {
        if (Objects.isNull(devFilePath)) return prodAuthor;
        else return devAuthor;
    }

    public String getCommitTime() {
        if (Objects.isNull(devFilePath))
            return Objects.nonNull(this.prodCommitTime) ? this.prodCommitTime.format(DateTimeFormatter.ISO_DATE) : null;
        else
            return Objects.nonNull(this.devCommitTime) ? this.devCommitTime.format(DateTimeFormatter.ISO_DATE) : null;
    }

    public String getMessage() {
        if (Objects.isNull(devFilePath)) return prodMessage;
        else return devMessage;
    }
}
