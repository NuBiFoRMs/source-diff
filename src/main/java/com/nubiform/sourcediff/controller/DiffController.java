package com.nubiform.sourcediff.controller;

import com.nubiform.sourcediff.constant.DiffType;
import com.nubiform.sourcediff.constant.SourceType;
import com.nubiform.sourcediff.service.DiffService;
import com.nubiform.sourcediff.service.DirectoryService;
import com.nubiform.sourcediff.service.HistoryService;
import com.nubiform.sourcediff.util.PathUtils;
import com.nubiform.sourcediff.vo.DiffResponse;
import com.nubiform.sourcediff.vo.SvnLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Controller
public class DiffController {

    public static final String VIEW_URI = "/view";
    public static final String REPOSITORY_PATH = "/{repository}";
    public static final String ANT_PATTERN = "/**";

    private final DirectoryService directoryService;
    private final DiffService diffService;
    private final HistoryService historyService;

    @GetMapping(VIEW_URI + REPOSITORY_PATH + ANT_PATTERN)
    public String view(@PathVariable String repository,
                       @RequestParam(required = false) SourceType revisedType,
                       @RequestParam(required = false) Long revised,
                       @RequestParam(required = false) SourceType originalType,
                       @RequestParam(required = false) Long original,
                       Pageable pageable,
                       Model model, HttpServletRequest request,
                       RedirectAttributes redirectAttributes) throws IOException {
        String path = PathUtils.removePrefix(request.getRequestURI(), VIEW_URI);
        log.info("request: {}, repository: {}, path: {}, revisedType: {}, revised: {}, originalType: {}, original: {}, pageable: {}", VIEW_URI, repository, path, revisedType, revised, originalType, original, pageable);

        // default type
        if (Objects.isNull(revisedType) || Objects.isNull(originalType)) {
            redirectAttributes.addAttribute("revisedType", SourceType.DEV);
            redirectAttributes.addAttribute("originalType", SourceType.PROD);
            redirectAttributes.addAttribute("revised", historyService.getLastRevision(path, SourceType.DEV));
            redirectAttributes.addAttribute("original", historyService.getLastRevision(path, SourceType.PROD));
            redirectAttributes.addAttribute("page", pageable.getPageNumber());
            return "redirect:" + VIEW_URI + path;
        }

        // default revision
        if (Objects.isNull(revised) || Objects.isNull(original)) {
            redirectAttributes.addAttribute("revisedType", revisedType);
            redirectAttributes.addAttribute("originalType", originalType);

            if (revisedType.equals(originalType)) {
                redirectAttributes.addAttribute("revised", Objects.isNull(revised) ? historyService.getLastRevision(path, revisedType) : revised);
                redirectAttributes.addAttribute("original", Objects.isNull(original) ? historyService.getLastRevision(path, revisedType) - 1 : original);
            } else {
                redirectAttributes.addAttribute("revised", Objects.isNull(revised) ? historyService.getLastRevision(path, revisedType) : revised);
                redirectAttributes.addAttribute("original", Objects.isNull(original) ? historyService.getLastRevision(path, originalType) : original);
            }
            redirectAttributes.addAttribute("page", pageable.getPageNumber());
            return "redirect:" + VIEW_URI + path;
        }

        // check revised revision
        if (revised <= -1 && !revised.equals(historyService.getLastRevision(path, revisedType))) {
            redirectAttributes.addAttribute("revisedType", revisedType);
            redirectAttributes.addAttribute("originalType", originalType);
            redirectAttributes.addAttribute("revised", historyService.getLastRevision(path, revisedType));
            redirectAttributes.addAttribute("original", original);
            redirectAttributes.addAttribute("page", pageable.getPageNumber());
            return "redirect:" + VIEW_URI + path;
        }

        // check original revision
        if (original <= -1 && !original.equals(historyService.getLastRevision(path, originalType))) {
            redirectAttributes.addAttribute("revisedType", revisedType);
            redirectAttributes.addAttribute("originalType", originalType);
            redirectAttributes.addAttribute("revised", revised);
            redirectAttributes.addAttribute("original", historyService.getLastRevision(path, originalType));
            redirectAttributes.addAttribute("page", pageable.getPageNumber());
            return "redirect:" + VIEW_URI + path;
        }

        List<SvnLogResponse> revisedRevision = historyService.getRevisionList(path, revisedType);
        List<SvnLogResponse> originalRevision = historyService.getRevisionList(path, originalType);

        Long newRevised = getNewRevised(revisedRevision, revised);
        Long newOriginal = getNewRevised(originalRevision, original);

        if (!revised.equals(newRevised) || !original.equals(newOriginal)) {
            redirectAttributes.addAttribute("revisedType", revisedType);
            redirectAttributes.addAttribute("originalType", originalType);
            redirectAttributes.addAttribute("revised", newRevised);
            redirectAttributes.addAttribute("original", newOriginal);
            return "redirect:" + VIEW_URI + path;
        }

        model.addAttribute("path", path);
        model.addAttribute("parentPath", directoryService.getParentPath(path));
        model.addAttribute("revisedType", revisedType);
        model.addAttribute("originalType", originalType);
        model.addAttribute("revised", revised);
        model.addAttribute("original", original);
        model.addAttribute("revisedRevision", revisedRevision);
        model.addAttribute("originalRevision", originalRevision);
        model.addAttribute("mode", revisedType.equals(originalType) ? SourceType.DEV.equals(revisedType) ? SourceType.DEV.name() : SourceType.PROD.name() : "DEFAULT");
        model.addAttribute("page", pageable.getPageNumber());

        List<DiffResponse> diffResponseList = diffService.getDiff(path, revisedType, revised, originalType, original);

        if (diffResponseList.stream()
                .anyMatch(diffResponse -> !DiffType.EQUAL.equals(diffResponse.getChangeType()) && !DiffType.SKIP.equals(diffResponse.getChangeType()))) {
            diffResponseList = diffService.setDiffView(diffResponseList);
            PageImpl<DiffResponse> pageableList = getPageableList(pageable, diffResponseList);
            log.debug("pageableList: {}, {}, {}", pageableList.getPageable().getPageNumber(), pageableList.getPageable().getPageSize(), pageableList.getTotalPages());

            model.addAttribute("diffList", diffService.getDiffList(pageableList.toList()));
            model.addAttribute("diff", pageableList);
            return "diff-view";
        } else {
            PageImpl<DiffResponse> pageableList = getPageableList(pageable, diffResponseList);
            log.debug("pageableList: {}, {}, {}", pageableList.getPageable().getPageNumber(), pageableList.getPageable().getPageSize(), pageableList.getTotalPages());

            model.addAttribute("diff", pageableList);
            return "view";
        }
    }

    private <T> PageImpl<T> getPageableList(Pageable pageable, List<T> list) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    private Long getNewRevised(List<SvnLogResponse> revisionList, Long revision) {
        return revisionList
                .stream()
                .map(SvnLogResponse::getRevision)
                .filter(r -> r <= revision)
                .findFirst().orElse(-1L);
    }
}
