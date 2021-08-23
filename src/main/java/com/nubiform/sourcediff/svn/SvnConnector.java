package com.nubiform.sourcediff.svn;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface SvnConnector {
    void checkout(String url, String revision, File location, String username, String password);

    Map<String, Object> log(File location);

    Map<String, Object> log(String url, String username, String password);

    List<Map<String, Object>> log(File location, int limit);

    List<Map<String, Object>> log(String url, String username, String password, int limit);

    long revisionLog(String url, String username, String password);

    long revisionLog(File location);

    void export(String url, String revision, File location, String username, String password);
}
