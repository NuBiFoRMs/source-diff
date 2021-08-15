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
        assertThat(highlight).isEqualTo("");
    }

}