package com.nubiform.sourcediff.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;

@RequiredArgsConstructor
@Component
public class FileFilter implements FilenameFilter {

    private final FileFilterProperties fileFilterProperties;

    @Override
    public boolean accept(File dir, String name) {
        name = StringUtils.lowerCase(name);

        for (String value : fileFilterProperties.getPrefix())
            if (StringUtils.startsWith(name, value)) return false;

        for (String value : fileFilterProperties.getPostfix())
            if (StringUtils.endsWith(name, value)) return false;

        for (String value : fileFilterProperties.getContains())
            if (StringUtils.contains(name, value)) return false;

        return true;
    }
}
