package com.nubiform.sourcediff.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(MailSender.class)
public class ConsoleMailSender implements MailSender {

    @Override
    public void send(MailMessage mailMessage) {
        System.out.println("========== Console Mail Sender ==========");
        System.out.println(mailMessage.getTo());
        System.out.println(mailMessage.getSubject());
        System.out.println(mailMessage.getMessage());
        System.out.println("========== Console Mail Sender ==========");
    }
}
