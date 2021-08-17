package com.nubiform.sourcediff.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "mailing", name = "enabled", havingValue = "true")
public class RestMailSender implements MailSender {

    private final MailingProperties mailingProperties;

    private final RestTemplate restTemplate;

    @Override
    public void send(MailMessage mailMessage) {
        RequestEntity<MailMessage> requestEntity = RequestEntity
                .method(HttpMethod.POST, mailingProperties.getHost())
                .body(mailMessage);
        restTemplate.exchange(requestEntity, String.class);
    }
}
