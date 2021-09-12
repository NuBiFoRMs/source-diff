package com.nubiform.sourcediff.constant;

import lombok.Getter;

@Getter
public enum SourceType {

    DEV("Dev"),
    PROD("Prod");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }
}
