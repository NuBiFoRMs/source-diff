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

    public String getDevCommitTime() {
        return this.devCommitTime.format(DateTimeFormatter.ISO_DATE);
    }

    public String getProdCommitTime() {
        return this.prodCommitTime.format(DateTimeFormatter.ISO_DATE);
    }
}
