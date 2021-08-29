package com.nubiform.sourcediff.svn;

import com.nubiform.sourcediff.constant.FileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Profile("!test")
@Component
public class SvnKitConn implements SvnConnector {

    private SVNClientManager getClientManager(String username, String password) {
        DefaultSVNOptions defaultOptions = SVNWCUtil.createDefaultOptions(true);
        ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(username, password.toCharArray());
        return SVNClientManager.newInstance(defaultOptions, authenticationManager);
    }

    @Override
    public void checkout(String url, String revision, File location, String username, String password) throws SvnException {
        try {
            getClientManager(username, password)
                    .getUpdateClient()
                    .doCheckout(SVNURL.parseURIEncoded(url), location, SVNRevision.parse(revision), null, SVNDepth.INFINITY, true);
        } catch (Exception e) {
            throw new SvnException("failed to svn checkout", e);
        }
    }

    @Override
    public void export(String url, String revision, File location, String username, String password) throws SvnException {
        try {
            getClientManager(username, password)
                    .getUpdateClient()
                    .doExport(SVNURL.parseURIEncoded(url), location, SVNRevision.UNDEFINED, SVNRevision.parse(revision), null, true, SVNDepth.INFINITY);
        } catch (Exception e) {
            throw new SvnException("failed to svn export", e);
        }
    }

    @Override
    public long getHeadRevision(String url, String username, String password) {
        try {
            return svnLog(url, null, "0", "HEAD", 1, username, password).get(0).getRevision();
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return -1;
    }

    @Override
    public long getBaseRevision(File location, String username, String password) {
        try {
            return svnLog(null, location, "0", "BASE", 1, username, password).get(0).getRevision();
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return -1;
    }

    @Override
    public SvnLog log(File location, String username, String password) {
        try {
            return svnLog(null, location, "0", "BASE", 1, username, password).get(0);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public List<SvnLog> log(File location, long limit, String username, String password) {
        try {
            return svnLog(null, location, "0", "BASE", limit, username, password);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public List<SvnLog> log(File location, String startRevision, String endRevision, String username, String password) {
        try {
            return svnLog(null, location, startRevision, endRevision, 0, username, password);
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return new ArrayList<>();
    }

    @Override
    public SvnInfo svnInfo(File location, String username, String password) {
        try {
            SVNInfo svnInfo = getClientManager(username, password)
                    .getWCClient()
                    .doInfo(location, SVNRevision.BASE);
            return SvnInfo.builder()
                    .url(svnInfo.getURL().toString())
                    .root(svnInfo.getRepositoryRootURL().toString())
                    .build();
        } catch (Exception e) {
            log.info("ignore exception: {}", e.getLocalizedMessage());
        }
        return null;
    }

    private List<SvnLog> svnLog(String url, File location, String startRevision, String endRevision, long limit, String username, String password) {
        List<SvnLog> svnLogList = new ArrayList<>();
        try {
            if (Objects.nonNull(url))
                getClientManager(username, password)
                        .getLogClient()
                        .doLog(SVNURL.parseURIEncoded(url),
                                null,
                                SVNRevision.UNDEFINED,
                                SVNRevision.parse(endRevision),
                                SVNRevision.parse(startRevision),
                                false,
                                true,
                                limit < 0 ? 0 : limit,
                                svnLogEntry -> svnLogList.add(map(svnLogEntry)));
            else if (Objects.nonNull(location))
                getClientManager(username, password)
                        .getLogClient()
                        .doLog(new File[]{location},
                                SVNRevision.parse(endRevision),
                                SVNRevision.parse(startRevision),
                                false,
                                true,
                                limit < 0 ? 0 : limit,
                                svnLogEntry -> svnLogList.add(map(svnLogEntry)));
        } catch (Exception e) {
            log.error("exception: {}", e.getLocalizedMessage());
        }
        return svnLogList;
    }

    private SvnLog map(SVNLogEntry svnLogEntry) {
        return SvnLog.builder()
                .revision(svnLogEntry.getRevision())
                .author(svnLogEntry.getAuthor())
                .date(svnLogEntry.getDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .message(StringUtils.left(svnLogEntry.getMessage(), 500))
                .path(svnLogEntry.getChangedPaths().entrySet()
                        .stream()
                        .map(Map.Entry::getValue)
                        .map(svnLogEntryPath -> SvnLog.Path.builder()
                                .fileType(svnLogEntryPath.getKind().equals(SVNNodeKind.FILE) ? FileType.FILE : svnLogEntryPath.getKind().equals(SVNNodeKind.DIR) ? FileType.DIRECTORY : null)
                                .action(String.valueOf(svnLogEntryPath.getType()))
                                .filePath(svnLogEntryPath.getPath())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
