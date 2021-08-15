package com.nubiform.sourcediff.vo;

import lombok.Data;

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

    private String prodFilePath;

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
}
