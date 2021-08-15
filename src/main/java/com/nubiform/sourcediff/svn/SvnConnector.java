package com.nubiform.sourcediff.svn;

import java.io.File;

public interface SvnConnector {
    void checkout(String url, String revision, File location, String username, String password);
}
