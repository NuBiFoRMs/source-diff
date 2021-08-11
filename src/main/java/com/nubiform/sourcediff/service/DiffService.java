package com.nubiform.sourcediff.service;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.DiffType;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.vo.DiffResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class DiffService {

    private final AppProperties appProperties;

    private final FileRepository fileRepository;

    public List<DiffResponse> diff(String path) throws IOException {
        FileEntity fileEntity = fileRepository.findByFilePathAndFileType(path, FileType.FILE)
                .orElseThrow(RuntimeException::new);

        List<String> devSource;
        List<String> prodSource;

        if (Objects.nonNull(fileEntity.getDevFilePath()) && Objects.nonNull(fileEntity.getProdFilePath())) {
            devSource = FileUtils.readLines(new File(fileEntity.getDevFilePath()), StandardCharsets.UTF_8);
            prodSource = FileUtils.readLines(new File(fileEntity.getProdFilePath()), StandardCharsets.UTF_8);
        } else if (Objects.nonNull(fileEntity.getDevFilePath()) && Objects.isNull(fileEntity.getProdFilePath())) {
            devSource = FileUtils.readLines(new File(fileEntity.getDevFilePath()), StandardCharsets.UTF_8);
            prodSource = devSource;
        } else if (Objects.isNull(fileEntity.getDevFilePath()) && Objects.nonNull(fileEntity.getProdFilePath())) {
            prodSource = FileUtils.readLines(new File(fileEntity.getProdFilePath()), StandardCharsets.UTF_8);
            devSource = prodSource;
        } else throw new RemoteException();

        DiffRowGenerator diffRowGenerator = DiffRowGenerator.create()
                .inlineDiffByWord(true)
                .showInlineDiffs(true)
                .ignoreWhiteSpaces(true)
                .build();

        List<DiffRow> diffRows = diffRowGenerator.generateDiffRows(prodSource, devSource);
        List<DiffResponse> diffResponseList = new ArrayList<>(diffRows.size());

        int line = 1;
        int oldLine = 1;
        int newLine = 1;

        for (DiffRow row : diffRows) {
            DiffResponse diffResponse = new DiffResponse();
            diffResponse.setLine(line++);

            if (DiffRow.Tag.CHANGE.equals(row.getTag())) {
                diffResponse.setChangeType(DiffType.UPDATE);
                diffResponse.setOldSource(row.getOldLine());
                diffResponse.setNewSource(row.getNewLine());
                diffResponse.setOldLine(oldLine++);
                diffResponse.setNewLine(newLine++);
            } else if (DiffRow.Tag.INSERT.equals(row.getTag())) {
                diffResponse.setChangeType(DiffType.CREATE);
                diffResponse.setNewSource(row.getNewLine());
                diffResponse.setNewLine(newLine++);
            } else if (DiffRow.Tag.DELETE.equals(row.getTag())) {
                diffResponse.setChangeType(DiffType.DELETE);
                diffResponse.setOldSource(row.getOldLine());
                diffResponse.setOldLine(oldLine++);
            } else if (DiffRow.Tag.EQUAL.equals(row.getTag())) {
                diffResponse.setChangeType(DiffType.EQUAL);
                diffResponse.setOldSource(row.getOldLine());
                diffResponse.setNewSource(row.getNewLine());
                diffResponse.setOldLine(oldLine++);
                diffResponse.setNewLine(newLine++);
            }

            log.debug("{}", diffResponse);
            diffResponseList.add(diffResponse);
        }

        return setSkip(setVisible(diffResponseList));
    }

    private List<DiffResponse> setVisible(List<DiffResponse> diffResponseList) {
        int showCount = 0;
        for (int i = 0; i < diffResponseList.size(); i++) {
            DiffResponse diffResponse = diffResponseList.get(i);

            if (!DiffType.EQUAL.equals(diffResponse.getChangeType()))
                showCount = appProperties.getShowLineCount() + 1;

            if (showCount > 0) {
                diffResponse.setVisible(true);
                showCount--;
            }
        }

        // reverse
        for (int i = diffResponseList.size() - 1; i >= 0; i--) {
            DiffResponse diffResponse = diffResponseList.get(i);

            if (!DiffType.EQUAL.equals(diffResponse.getChangeType()))
                showCount = appProperties.getShowLineCount() + 1;

            if (showCount > 0) {
                diffResponse.setVisible(true);
                showCount--;
            }
        }

        return diffResponseList;
    }

    private List<DiffResponse> setSkip(List<DiffResponse> diffResponseList) {
        for (int i = 0; i < diffResponseList.size() - 1; i++) {
            DiffResponse current = diffResponseList.get(i);
            DiffResponse next = diffResponseList.get(i + 1);

            if (current.isVisible() != next.isVisible()) {
                if (current.isVisible()) {
                    next.setSkip(true);
                } else {
                    current.setSkip(true);
                }
            }
        }

        return diffResponseList;
    }
}