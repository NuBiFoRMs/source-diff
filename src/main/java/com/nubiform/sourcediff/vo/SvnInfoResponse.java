package com.nubiform.sourcediff.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SvnInfoResponse {

    private Long revision;

    private String author;

    private LocalDateTime commitTime;

    private String message;

    public String getSvnInfo() {
        return String.format("%s [%s]", this.revision, this.commitTime.format(DateTimeFormatter.ISO_DATE));
    }
}
