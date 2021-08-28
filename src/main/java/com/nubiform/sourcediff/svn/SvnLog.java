package com.nubiform.sourcediff.svn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SvnLog {

    private long revision;

    private String author;

    private LocalDateTime date;

    private String message;
}
