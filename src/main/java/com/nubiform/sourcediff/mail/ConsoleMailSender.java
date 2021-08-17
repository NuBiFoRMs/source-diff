package com.nubiform.sourcediff.mail;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("test")
@Service
public class ConsoleMailSender implements MailSender {

    @Override
    public void send(MailMessage mailMessage) {
        System.out.println(mailMessage.getTo());
        System.out.println(mailMessage.getSubject());
        System.out.println(mailMessage.getMessage());
    }
}
