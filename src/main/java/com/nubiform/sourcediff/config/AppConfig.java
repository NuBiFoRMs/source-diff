package com.nubiform.sourcediff.config;

import com.nubiform.sourcediff.mail.ConsoleMailSender;
import com.nubiform.sourcediff.mail.MailSender;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setSourceNameTokenizer(NameTokenizers.UNDERSCORE)
                .setDestinationNameTokenizer(NameTokenizers.UNDERSCORE);
        return modelMapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @ConditionalOnMissingBean(MailSender.class)
    @Bean
    public MailSender consoleMailSender() {
        return new ConsoleMailSender();
    }
}
