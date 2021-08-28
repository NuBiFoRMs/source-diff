package com.nubiform.sourcediff.svn;

import java.io.File;
import java.util.List;

public interface SvnConnector {
    void checkout(String url, String revision, File location, String username, String password) throws SvnException;

    void export(String url, String revision, File location, String username, String password) throws SvnException;

    long getHeadRevision(String url, String username, String password);

    long getBaseRevision(File location, String username, String password);

    SvnLog log(File location, String username, String password);

    List<SvnLog> log(File location, long limit, String username, String password);
}
