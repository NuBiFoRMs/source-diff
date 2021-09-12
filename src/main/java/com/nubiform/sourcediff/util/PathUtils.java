package com.nubiform.sourcediff.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;

@UtilityClass
public class PathUtils {

    public static final String SEPARATOR = "/";

    public static String removePrefix(String path, String prefix) {
        return removeLastSeparator(StringUtils.substring(path, StringUtils.indexOf(path, prefix) + StringUtils.length(prefix)));
    }

    public static String replaceSeparator(String path) {
        return StringUtils.replace(path, "\\", SEPARATOR);
    }

    public static String connectPath(String path1, String path2) {
        if (Objects.nonNull(path1) && Objects.nonNull(path2))
            return removeLastSeparator(removeLastSeparator(path1) + SEPARATOR + removeFirstSeparator(path2));
        else if (Objects.isNull(path1) && Objects.isNull(path2))
            return null;
        else if (Objects.nonNull(path1))
            return removeLastSeparator(path1);
        else
            return removeLastSeparator(path2);
    }

    public static String removeLastSeparator(String path) {
        path = replaceSeparator(path);

        while (StringUtils.lastIndexOf(path, SEPARATOR) != StringUtils.INDEX_NOT_FOUND &&
                StringUtils.lastIndexOf(path, SEPARATOR) == StringUtils.length(path) - 1) {
            path = StringUtils.substring(path, 0, StringUtils.lastIndexOf(path, SEPARATOR));
        }
        return path;
    }

    public static String removeFirstSeparator(String path) {
        path = replaceSeparator(path);

        while (StringUtils.indexOf(path, SEPARATOR) != StringUtils.INDEX_NOT_FOUND &&
                StringUtils.indexOf(path, SEPARATOR) == 0) {
            path = StringUtils.substring(path, 1);
        }
        return path;
    }

    public static String highlight(String path, String keyword, Function<String, String> function) {
        if (path == null)
            return null;

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
