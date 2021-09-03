package com.nubiform.sourcediff.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SvnInfoResponse {

    public static final int LENGTH = 50;

    private Long revision;

    private String action;

    private String author;

    private LocalDateTime commitTime;

    private String message;

    public String getSvnInfo() {
        return String.format("%d [%s : %s] %s", this.revision, this.commitTime.format(DateTimeFormatter.ISO_DATE), this.author, this.getShortMessage());
    }

    public String getShortMessage() {
        if (StringUtils.length(this.message) >= LENGTH + 5)
            return StringUtils.left(this.message, LENGTH) + "...";
        else
            return this.message;
    }
}
