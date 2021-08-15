package com.nubiform.sourcediff.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

@UtilityClass
public class PathUtils {

    public static final String SEPARATOR = "/";

    public static String removePrefix(String path, String prefix) {
        return StringUtils.substring(path, StringUtils.indexOf(path, prefix) + StringUtils.length(prefix));
    }

    public static String replaceSeparator(String path) {
        return StringUtils.replace(path, "\\", SEPARATOR);
    }

    public static String highlight(String path, String keyword, Function<String, String> function) {
        if (path == null)
            return "";

        int size = StringUtils.length(keyword);
        int start;
        int prevEnd = 0;
        int end = 0;

        StringBuilder result = new StringBuilder();
        while ((start = StringUtils.indexOfIgnoreCase(path, keyword, end)) != StringUtils.INDEX_NOT_FOUND) {
            end = start + size;

            String preKeyword = StringUtils.substring(path, prevEnd, start);
            String thisKeyword = StringUtils.substring(path, start, end);
            result.append(preKeyword).append(function.apply(thisKeyword));

            prevEnd = end;
        }
        String postKeyword = StringUtils.substring(path, prevEnd);
        result.append(postKeyword);

        return result.toString();
    }
}
