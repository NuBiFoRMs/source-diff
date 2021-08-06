package com.nubiform.sourcediff.svn;

import com.nubiform.sourcediff.util.SvnUtils;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CommandLineSvn implements SvnConnector {

    @Override
    public void checkout(String url, String revision, File location, String username, String password) {
        SvnUtils.checkout(url, revision, location, username, password);
    }
}
