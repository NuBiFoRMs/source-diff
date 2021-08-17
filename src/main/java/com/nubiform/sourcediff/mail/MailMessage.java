package com.nubiform.sourcediff.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MailMessage {

    private List<String> to;

    private String subject;

    private String message;
}
