package com.nubiform.sourcediff.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Test
    void highlight() {
        String highlight = PathUtils.highlight("asearchbSearchc", "search", s -> "<i>" + s + "</i>");
        assertThat(highlight).isEqualTo("a<i>search</i>b<i>Search</i>c");

        highlight = PathUtils.highlight("asearchbSearch", "search", s -> "<i>" + s + "</i>");
        assertThat(highlight).isEqualTo("a<i>search</i>b<i>Search</i>");

        highlight = PathUtils.highlight("abc", "search", s -> "<i>" + s + "</i>");
        assertThat(highlight).isEqualTo("abc");

        highlight = PathUtils.highlight("", "search", s -> "<i>" + s + "</i>");
        assertThat(highlight).isEqualTo("");

        highlight = PathUtils.highlight(null, "search", s -> "<i>" + s + "</i>");
        assertThat(highlight).isNull();
    }

    @Test
    void removeFirstSeparator() {
        String path = "/test/";
        assertThat(PathUtils.removeFirstSeparator(path)).isEqualTo("test/");
    }

    @Test
    void removeLastSeparator() {
        String path = "/test/";
        assertThat(PathUtils.removeLastSeparator(path)).isEqualTo("/test");
    }

    @Test
    void connectPath() {
        String path1 = "/test1/";
        String path2 = "/test2/";
        assertThat(PathUtils.connectPath(path1, path2)).isEqualTo("/test1/test2");
    }

    @Test
    void connectPathNull() {
        String path1 = "/test1/";
        String path2 = "/test2/";

        assertThat(PathUtils.connectPath(null, null)).isNull();
        assertThat(PathUtils.connectPath(path1, null)).isEqualTo("/test1");
        assertThat(PathUtils.connectPath(null, path2)).isEqualTo("/test2");
    }
}