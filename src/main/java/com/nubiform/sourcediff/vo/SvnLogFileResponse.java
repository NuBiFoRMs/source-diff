package com.nubiform.sourcediff.vo;

import lombok.Data;

@Data
public class SvnLogFileResponse extends SvnLogResponse {

    private String filePath;

    private String fileType;

    private String action;
}
