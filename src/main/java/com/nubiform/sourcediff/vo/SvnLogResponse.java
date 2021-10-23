package com.nubiform.sourcediff.vo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class SvnLogResponse {

    private static final int LENGTH = 50;

    private Long revision;

    private String author;

    private LocalDateTime commitTime;

    private String message;

    public String getCommitTime() {
        return this.commitTime.format(DateTimeFormatter.ISO_DATE);
    }

    public String getSvnInfo() {
        return String.format("%d [%s : %s] %s", this.revision, this.getCommitTime(), this.author, this.getShortMessage());
    }

    public String getShortMessage() {
        if (StringUtils.length(this.message) == 0)
            return "no commit message";
        else if (StringUtils.length(this.message) >= LENGTH + 5)
            return StringUtils.left(this.message, LENGTH) + "...";
        else
            return this.message;
    }
}
