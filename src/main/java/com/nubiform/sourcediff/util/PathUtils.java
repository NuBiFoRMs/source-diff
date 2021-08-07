package com.nubiform.sourcediff.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class PathUtils {

    public static String removePrefix(String path, String prefix) {
        return StringUtils.substring(path, StringUtils.indexOf(path, prefix) + StringUtils.length(prefix));
    }

    public static String replaceSeparator(String path) {
        return StringUtils.replace(path, "\\", "/");
    }
}
