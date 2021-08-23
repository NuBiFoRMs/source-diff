package com.nubiform.sourcediff.service;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.nubiform.sourcediff.config.AppProperties;
import com.nubiform.sourcediff.constant.DiffType;
import com.nubiform.sourcediff.constant.FileType;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.repository.FileEntity;
import com.nubiform.sourcediff.repository.FileRepository;
import com.nubiform.sourcediff.vo.DiffResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class DiffService {

    private final AppProperties appProperties;

    private final HistoryService historyService;

    private final FileRepository fileRepository;

    public List<DiffResponse> getDiff(String path, String devRevision, String prodRevision) throws IOException {
        FileEntity fileEntity = fileRepository.findByFilePathAndFileType(path, FileType.FILE)
                .orElseThrow(RuntimeException::new);

        List<String> devSource = null;
        List<String> prodSource = null;

        if (StringUtils.isNotBlank(devRevision)) {
            devSource = historyService.exportFile(path, SourceType.DEV, devRevision);
        } else if (Objects.nonNull(fileEntity.getDevFilePath())) {
            devSource = FileUtils.readLines(new File(fileEntity.getDevFilePath()), StandardCharsets.UTF_8);
        }

        if (StringUtils.isNotBlank(prodRevision)) {
            prodSource = historyService.exportFile(path, SourceType.PROD, prodRevision);
        } else if (Objects.nonNull(fileEntity.getProdFilePath())) {
            prodSource = FileUtils.readLines(new File(fileEntity.getProdFilePath()), StandardCharsets.UTF_8);
        }

        if (Objects.isNull(devSource) && Objects.isNull(prodSource))
            throw new RuntimeException();
        else if (Objects.nonNull(devSource) && Objects.isNull(prodSource))
            prodSource = devSource;
        else if (Objects.isNull(devSource) && Objects.nonNull(prodSource))
            devSource = prodSource;

        return getDiff(devSource, prodSource);
    }

    public List<DiffResponse> getDiff(List<String> devSource, List<String> prodSource) {
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

        return diffResponseList;
    }

    public List<DiffResponse> setDiffView(List<DiffResponse> diffResponseList) {
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
        int keyLine = 1;
        int line = Integer.MAX_VALUE;
        int oldLine = Integer.MAX_VALUE;
        int newLine = Integer.MAX_VALUE;

        List<DiffResponse> result = new ArrayList<>();
        for (int i = 0; i < diffResponseList.size(); i++) {
            DiffResponse diffResponse = diffResponseList.get(i);

            if (diffResponse.isVisible()) {
                if (line < Integer.MAX_VALUE) {
                    DiffResponse skipResponse = new DiffResponse();
                    skipResponse.setChangeType(DiffType.SKIP);
                    skipResponse.setLine(keyLine++);
                    skipResponse.setOldSource(String.format("[%d - %d]", oldLine, diffResponse.getOldLine() - 1));
                    skipResponse.setNewSource(String.format("[%d - %d]", newLine, diffResponse.getNewLine() - 1));
                    skipResponse.setVisible(true);
                    result.add(skipResponse);
                }

                diffResponse.setLine(keyLine++);
                result.add(diffResponse);

                line = Integer.MAX_VALUE;
                oldLine = Integer.MAX_VALUE;
                newLine = Integer.MAX_VALUE;
            } else {
                line = Math.min(diffResponse.getLine(), line);
                oldLine = Math.min(diffResponse.getOldLine(), oldLine);
                newLine = Math.min(diffResponse.getNewLine(), newLine);
            }
        }

        if (line < Integer.MAX_VALUE) {
            DiffResponse skipResponse = new DiffResponse();
            skipResponse.setChangeType(DiffType.SKIP);
            skipResponse.setOldSource(String.format("[%d - %d]", oldLine, diffResponseList.get(diffResponseList.size() - 1).getOldLine() - 1));
            skipResponse.setNewSource(String.format("[%d - %d]", newLine, diffResponseList.get(diffResponseList.size() - 1).getNewLine() - 1));
            skipResponse.setVisible(true);
            result.add(skipResponse);
        }

        return result;
    }

    public List<Integer> getDiffList(List<DiffResponse> diffResponseList) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < diffResponseList.size() - 1; i++) {
            DiffResponse current = diffResponseList.get(i);
            DiffResponse next = diffResponseList.get(i + 1);

            if (i == 0 &&
                    !DiffType.EQUAL.equals(current.getChangeType()) && !DiffType.SKIP.equals(current.getChangeType())) {
                result.add(current.getLine());
            }

            if (current.getChangeType() != next.getChangeType() &&
                    (!DiffType.EQUAL.equals(next.getChangeType()) && !DiffType.SKIP.equals(next.getChangeType()))) {
                result.add(next.getLine());
            }
        }
        return result;
    }
}
