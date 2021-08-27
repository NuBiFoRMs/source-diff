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

    private String revision;

    private String author;

    private LocalDateTime date;

    private String message;

    public String getSvnInfo() {
        return String.format("%s [%s]", this.revision, this.date.format(DateTimeFormatter.ISO_DATE));
    }
}
