package com.nubiform.sourcediff.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties("file-filter")
public class FileFilterProperties {

    private List<String> prefix;

    private List<String> postfix;

    private List<String> contains;
}
