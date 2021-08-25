package com.nubiform.sourcediff.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties("source-diff")
public class AppProperties {

    private String host;

    private int showLineCount;

    private List<RepositoryProperties> repositories;

    @Data
    public static class RepositoryProperties {

        private String name;

        private String devUrl;

        private String devUsername;

        private String devPassword;

        private String prodUrl;

        private String prodUsername;

        private String prodPassword;

        private List<String> receivers;
    }
}
