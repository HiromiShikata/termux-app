package com.termux.shared.markdown;

import org.junit.Assert;
import org.junit.Test;

public class MarkdownUtilsTest {

    @Test
    public void getMarkdownCodeForStringReturnsNullForNull() {
        Assert.assertNull(MarkdownUtils.getMarkdownCodeForString(null, false));
    }

    @Test
    public void getMarkdownCodeForStringReturnsEmptyForEmpty() {
        Assert.assertEquals("", MarkdownUtils.getMarkdownCodeForString("", true));
    }

    @Test
    public void getMarkdownCodeForStringInlineWrapsWithSingleBacktick() {
        Assert.assertEquals("`code`", MarkdownUtils.getMarkdownCodeForString("code", false));
    }

    @Test
    public void getMarkdownCodeForStringInlineUsesMoreBackticksThanContent() {
        Assert.assertEquals("``a`b``", MarkdownUtils.getMarkdownCodeForString("a`b", false));
    }

    @Test
    public void getMarkdownCodeForStringInlinePadsLeadingBacktick() {
        Assert.assertEquals("`` `lead``", MarkdownUtils.getMarkdownCodeForString("`lead", false));
    }

    @Test
    public void getMarkdownCodeForStringInlinePadsTrailingBacktick() {
        Assert.assertEquals("``trail` ``", MarkdownUtils.getMarkdownCodeForString("trail`", false));
    }

    @Test
    public void getMarkdownCodeForStringBlockWrapsWithFencedBackticks() {
        Assert.assertEquals("```\ncode\n```", MarkdownUtils.getMarkdownCodeForString("code", true));
    }

    @Test
    public void getMaxConsecutiveBackTicksCountZeroForNull() {
        Assert.assertEquals(0, MarkdownUtils.getMaxConsecutiveBackTicksCount(null));
    }

    @Test
    public void getMaxConsecutiveBackTicksCountZeroForEmpty() {
        Assert.assertEquals(0, MarkdownUtils.getMaxConsecutiveBackTicksCount(""));
    }

    @Test
    public void getMaxConsecutiveBackTicksCountZeroWhenNoBackticks() {
        Assert.assertEquals(0, MarkdownUtils.getMaxConsecutiveBackTicksCount("plain text"));
    }

    @Test
    public void getMaxConsecutiveBackTicksCountReturnsLongestRun() {
        Assert.assertEquals(3, MarkdownUtils.getMaxConsecutiveBackTicksCount("a`b``c```d"));
    }

    @Test
    public void getLiteralSingleLineMarkdownStringEntryUsesObjectValue() {
        Assert.assertEquals("**Key**: value  ",
            MarkdownUtils.getLiteralSingleLineMarkdownStringEntry("Key", "value", "def"));
    }

    @Test
    public void getLiteralSingleLineMarkdownStringEntryUsesDefaultWhenNull() {
        Assert.assertEquals("**Key**: def  ",
            MarkdownUtils.getLiteralSingleLineMarkdownStringEntry("Key", null, "def"));
    }

    @Test
    public void getSingleLineMarkdownStringEntryWrapsValueAsInlineCode() {
        Assert.assertEquals("**Key**: `value`  ",
            MarkdownUtils.getSingleLineMarkdownStringEntry("Key", "value", "def"));
    }

    @Test
    public void getSingleLineMarkdownStringEntryUsesDefaultWhenNull() {
        Assert.assertEquals("**Key**: def  ",
            MarkdownUtils.getSingleLineMarkdownStringEntry("Key", null, "def"));
    }

    @Test
    public void getMultiLineMarkdownStringEntryWrapsValueAsCodeBlock() {
        Assert.assertEquals("**Key**:\n```\nvalue\n```\n",
            MarkdownUtils.getMultiLineMarkdownStringEntry("Key", "value", "def"));
    }

    @Test
    public void getMultiLineMarkdownStringEntryUsesDefaultWhenNull() {
        Assert.assertEquals("**Key**: def\n",
            MarkdownUtils.getMultiLineMarkdownStringEntry("Key", null, "def"));
    }

    @Test
    public void getLinkMarkdownStringBuildsMarkdownLink() {
        Assert.assertEquals("[label](https://example.com)",
            MarkdownUtils.getLinkMarkdownString("label", "https://example.com"));
    }

    @Test
    public void getLinkMarkdownStringEscapesClosingBracketInLabel() {
        Assert.assertEquals("[a\\]b](https://example.com)",
            MarkdownUtils.getLinkMarkdownString("a]b", "https://example.com"));
    }

    @Test
    public void getLinkMarkdownStringEscapesClosingParenInUrl() {
        Assert.assertEquals("[label](https://example.com/a\\)b)",
            MarkdownUtils.getLinkMarkdownString("label", "https://example.com/a)b"));
    }

    @Test
    public void getLinkMarkdownStringReturnsLabelWhenUrlNull() {
        Assert.assertEquals("label", MarkdownUtils.getLinkMarkdownString("label", null));
    }
}
