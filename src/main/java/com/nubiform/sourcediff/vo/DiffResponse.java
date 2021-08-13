package com.nubiform.sourcediff.vo;

import lombok.Data;

@Data
public class DiffResponse {

    private Integer line;

    private String changeType;

    private Integer oldLine;

    private String oldSource;

    private Integer newLine;

    private String newSource;

    private boolean visible = false;

    private boolean skip = false;
}
