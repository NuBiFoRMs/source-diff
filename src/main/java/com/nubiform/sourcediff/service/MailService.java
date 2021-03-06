package com.nubiform.sourcediff.service;

import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.mail.MailMessage;
import com.nubiform.sourcediff.mail.MailSender;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
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

    private final ModelMapper modelMapper;

    public void mailing(String repository) {
        List<FileResponse> files = fileRepository.findAllByFilePathStartsWith(PathUtils.SEPARATOR + repository, Sort.by("filePath", "fileType"))
                .stream()
                .filter(file -> file.getDiffCount() > 0 || Objects.isNull(file.getDevFilePath()) || Objects.isNull(file.getProdFilePath()))
                .filter(file -> FileType.FILE.equals(file.getFileType()))
                .map(this::map)
                .collect(Collectors.toList());

        Context context = new Context();
        context.setVariable("host", appProperties.getHost());
        context.setVariable("files", files);

        String message = templateEngine.process("mail", context);
        log.debug("message:\n{}", message);

        MailMessage mailMessage = MailMessage.builder()
                .to(appProperties.getRepository(repository).getReceivers())
                .subject("Source Diff. [" + repository + "]")
                .message(message)
                .build();
        mailSender.send(mailMessage);
    }

    private FileResponse map(FileEntity fileEntity) {
        FileResponse fileResponse = modelMapper.map(fileEntity, FileResponse.class);
        fileResponse.setFilePathDisplay(fileResponse.getFilePath());
        return fileResponse;
    }
}
