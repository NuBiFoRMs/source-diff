package com.nubiform.sourcediff.config;

import com.nubiform.sourcediff.constant.SourceType;
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

        public String getUrl(SourceType sourceType) {
            return SourceType.DEV.equals(sourceType) ? devUrl : prodUrl;
        }

        public String getUsername(SourceType sourceType) {
            return SourceType.DEV.equals(sourceType) ? devUsername : prodUsername;
        }

        public String getPassword(SourceType sourceType) {
            return SourceType.DEV.equals(sourceType) ? devPassword : prodPassword;
        }
    }

    public AppProperties.RepositoryProperties getRepository(String repository) {
        return this.getRepositories()
                .stream()
                .filter(repo -> repo.getName().equals(repository))
                .findFirst()
                .orElseThrow(RuntimeException::new);
    }
}
