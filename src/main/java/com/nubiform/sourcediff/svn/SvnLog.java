package com.nubiform.sourcediff.svn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SvnLog {

    private long revision;

    private String author;

    private LocalDateTime date;

    private String message;

    private List<Path> path;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class Path {

        private String fileType;

        private String action;

        private String filePath;
    }
}
