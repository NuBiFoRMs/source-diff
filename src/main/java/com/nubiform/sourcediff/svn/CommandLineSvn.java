package com.nubiform.sourcediff.svn;

import com.nubiform.sourcediff.util.SvnUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class CommandLineSvn implements SvnConnector {

    @Override
    public void checkout(String url, String revision, File location, String username, String password) {
        SvnUtils.checkout(url, revision, location, username, password);
    }

    @Override
    public Map<String, Object> log(File location) {
        return SvnUtils.log(location);
    }

    @Override
    public Map<String, Object> log(String url, String username, String password) {
        return SvnUtils.log(url, username, password);
    }

    @Override
    public List<Map<String, Object>> log(File location, int limit) {
        return SvnUtils.log(location, limit);
    }

    @Override
    public List<Map<String, Object>> log(String url, String username, String password, int limit) {
        return SvnUtils.log(url, username, password, limit);
    }

    @Override
    public long revisionLog(String url, String username, String password) {
        Map<String, Object> svnLog = SvnUtils.log(url, username, password);
        if (Objects.nonNull(svnLog))
            return Long.parseLong((String) svnLog.get(SvnUtils.REVISION));
        else
            return 0;
    }

    @Override
    public long revisionLog(File location) {
        Map<String, Object> svnLog = SvnUtils.log(location);
        if (Objects.nonNull(svnLog))
            return Long.parseLong((String) svnLog.get(SvnUtils.REVISION));
        else
            return 0;
    }

    @Override
    public void export(String url, String revision, File location, String username, String password) {
        SvnUtils.export(url, revision, username, password, location);
    }
}
