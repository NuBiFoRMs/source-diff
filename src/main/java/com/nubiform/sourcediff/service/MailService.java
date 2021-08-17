package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.mail.MailMessage;
import com.nubiform.sourcediff.mail.MailSender;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailService {

    private final AppProperties appProperties;

    private final FileRepository fileRepository;

    private final TemplateEngine templateEngine;

    private final MailSender mailSender;

    public void mailing(String repository) {
        List<FileEntity> files = fileRepository.findAllByFilePathStartsWith(PathUtils.SEPARATOR + repository);

        files = files
                .stream()
                .filter(file -> file.getDiffCount() > 0)
                .filter(file -> FileType.FILE.equals(file.getFileType()))
                .collect(Collectors.toList());

        Context context = new Context();
        context.setVariable("host", appProperties.getHost());
        context.setVariable("files", files);

        String message = templateEngine.process("mail", context);
        log.debug("message:\n{}", message);

        MailMessage mailMessage = MailMessage.builder()
                .to(appProperties.getReceivers())
                .subject("Source Diff. [" + repository + "]")
                .message(message)
                .build();
        mailSender.send(mailMessage);
    }
}
