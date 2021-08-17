package com.nubiform.sourcediff.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("mailing")
public class MailingProperties {

    private boolean enabled;

    private String host;
}
