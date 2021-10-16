package com.nubiform.sourcediff.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SvnLogFileResponse {

    public static final int LENGTH = 50;

    private Long revision;

    private String author;

    private LocalDateTime commitTime;

    private String message;

    private String filePath;

    public String getShortMessage() {
        if (StringUtils.length(this.message) == 0)
            return "no commit message";
        else if (StringUtils.length(this.message) >= LENGTH + 5)
            return StringUtils.left(this.message, LENGTH) + "...";
        else
            return this.message;
    }
}
