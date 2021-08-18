package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.mail.MailMessage;
import com.nubiform.sourcediff.mail.MailSender;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.svn.SvnConnector;
import com.nubiform.sourcediff.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class MailService {

    private final AppProperties appProperties;

    private final FileRepository fileRepository;

    private final TemplateEngine templateEngine;

    private final MailSender mailSender;

    private final SvnConnector svnConnector;

    public void mailing(String repository) {
        List<FileEntity> files = fileRepository.findAllByFilePathStartsWith(PathUtils.SEPARATOR + repository);

        files = files
                .stream()
                .filter(file -> file.getDiffCount() > 0)
                .filter(file -> FileType.FILE.equals(file.getFileType()))
                .map(this::updateSvnInfo)
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

    private FileEntity updateSvnInfo(FileEntity file) {
        if (Objects.isNull(file.getInfoModified()) ||
                (Objects.nonNull(file.getDevFilePath()) && file.getInfoModified().isBefore(file.getDevModified())) ||
                (Objects.nonNull(file.getProdFilePath()) && file.getInfoModified().isBefore(file.getProdModified()))) {

            // svn info.
            if (Objects.nonNull(file.getDevFilePath())) {
                Map<String, Object> svnInfo = svnConnector.log(new File(file.getDevFilePath()));
                log.debug("svnInfo: {}", svnInfo);
                file.setDevRevision((String) svnInfo.get("revision"));
                file.setDevMessage((String) svnInfo.get("msg"));
                file.setDevCommitTime((LocalDateTime) svnInfo.get("date"));
                file.setDevAuthor((String) svnInfo.get("author"));
            }
            if (Objects.nonNull(file.getProdFilePath())) {
                Map<String, Object> svnInfo = svnConnector.log(new File(file.getProdFilePath()));
                log.debug("svnInfo: {}", svnInfo);
                file.setProdRevision((String) svnInfo.get("revision"));
                file.setProdMessage((String) svnInfo.get("msg"));
                file.setProdCommitTime((LocalDateTime) svnInfo.get("date"));
                file.setProdAuthor((String) svnInfo.get("author"));
            }

            file.setInfoModified(LocalDateTime.now());
        }

        return fileRepository.save(file);
    }
}
