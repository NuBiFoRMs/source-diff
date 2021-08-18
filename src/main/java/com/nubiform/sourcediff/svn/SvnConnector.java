package com.nubiform.sourcediff.svn;

import java.io.File;
import java.util.Map;

public interface SvnConnector {
    void checkout(String url, String revision, File location, String username, String password);

    Map<String, Object> log(File location);
}
