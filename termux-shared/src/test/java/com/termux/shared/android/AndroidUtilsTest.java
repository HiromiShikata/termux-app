package com.termux.shared.android;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

public class AndroidUtilsTest {

    @Test
    public void getPropertyMarkdownWrapsValueAsInlineCode() {
        Assert.assertEquals("**LABEL**: `value`  ", AndroidUtils.getPropertyMarkdown("LABEL", "value"));
    }

    @Test
    public void getPropertyMarkdownUsesDashDefaultForNullValue() {
        Assert.assertEquals("**LABEL**: -  ", AndroidUtils.getPropertyMarkdown("LABEL", null));
    }

    @Test
    public void getLiteralPropertyMarkdownDoesNotWrapValue() {
        Assert.assertEquals("**LABEL**: value  ", AndroidUtils.getLiteralPropertyMarkdown("LABEL", "value"));
    }

    @Test
    public void getLiteralPropertyMarkdownUsesDashDefaultForNullValue() {
        Assert.assertEquals("**LABEL**: -  ", AndroidUtils.getLiteralPropertyMarkdown("LABEL", null));
    }

    @Test
    public void appendPropertyToMarkdownPrependsNewline() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdown(builder, "LABEL", "value");
        Assert.assertEquals("\n**LABEL**: `value`  ", builder.toString());
    }

    @Test
    public void appendPropertyToMarkdownAppendsNullValueAsDash() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdown(builder, "LABEL", null);
        Assert.assertEquals("\n**LABEL**: -  ", builder.toString());
    }

    @Test
    public void appendLiteralPropertyToMarkdownPrependsNewline() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendLiteralPropertyToMarkdown(builder, "LABEL", "value");
        Assert.assertEquals("\n**LABEL**: value  ", builder.toString());
    }

    @Test
    public void appendPropertyToMarkdownIfSetSkipsNullValue() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "LABEL", null);
        Assert.assertEquals(0, builder.length());
    }

    @Test
    public void appendPropertyToMarkdownIfSetSkipsEmptyStringValue() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "LABEL", "");
        Assert.assertEquals(0, builder.length());
    }

    @Test
    public void appendPropertyToMarkdownIfSetSkipsRelValue() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "LABEL", "REL");
        Assert.assertEquals(0, builder.length());
    }

    @Test
    public void appendPropertyToMarkdownIfSetAppendsNonEmptyValue() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "LABEL", "value");
        Assert.assertEquals("\n**LABEL**: `value`  ", builder.toString());
    }

    @Test
    public void getCurrentTimeStampMatchesUtcSecondFormat() {
        Assert.assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} UTC",
            AndroidUtils.getCurrentTimeStamp()));
    }

    @Test
    public void getCurrentMilliSecondUtcTimeStampMatchesMillisecondFormat() {
        Assert.assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} UTC",
            AndroidUtils.getCurrentMilliSecondUTCTimeStamp()));
    }

    @Test
    public void getCurrentMilliSecondLocalTimeStampMatchesUnderscoreFormat() {
        Assert.assertTrue(Pattern.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}",
            AndroidUtils.getCurrentMilliSecondLocalTimeStamp()));
    }
}
