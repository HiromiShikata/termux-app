package com.termux.shared.android;

import com.termux.shared.logger.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.regex.Pattern;

public class AndroidUtilsTimestampAndMarkdownTest {

    private static final Pattern UTC_SECONDS = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} UTC");
    private static final Pattern UTC_MILLIS = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} UTC");
    private static final Pattern LOCAL_MILLIS = Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}");

    private static int originalLogLevel;

    @BeforeClass
    public static void silenceAndroidLog() throws Exception {
        Field field = Logger.class.getDeclaredField("CURRENT_LOG_LEVEL");
        field.setAccessible(true);
        originalLogLevel = field.getInt(null);
        field.setInt(null, Logger.LOG_LEVEL_OFF);
    }

    @AfterClass
    public static void restoreAndroidLog() throws Exception {
        Field field = Logger.class.getDeclaredField("CURRENT_LOG_LEVEL");
        field.setAccessible(true);
        field.setInt(null, originalLogLevel);
    }

    @Test
    public void getPropertyMarkdownIncludesLabelAndValue() {
        String markdown = AndroidUtils.getPropertyMarkdown("Version", "1.2.3");
        Assert.assertTrue(markdown.contains("Version"));
        Assert.assertTrue(markdown.contains("1.2.3"));
    }

    @Test
    public void getLiteralPropertyMarkdownIncludesLabelAndValue() {
        String markdown = AndroidUtils.getLiteralPropertyMarkdown("Build", "abc");
        Assert.assertTrue(markdown.contains("Build"));
        Assert.assertTrue(markdown.contains("abc"));
    }

    @Test
    public void appendPropertyToMarkdownAlwaysAppends() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdown(builder, "Label", "value");
        Assert.assertTrue(builder.toString().contains("Label"));
        Assert.assertTrue(builder.toString().contains("value"));
    }

    @Test
    public void appendLiteralPropertyToMarkdownAppends() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendLiteralPropertyToMarkdown(builder, "Literal", "raw_value");
        Assert.assertTrue(builder.toString().contains("Literal"));
        Assert.assertTrue(builder.toString().contains("raw_value"));
    }

    @Test
    public void appendPropertyToMarkdownIfSetSkipsNullEmptyAndRel() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "Null", null);
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "Empty", "");
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "Rel", "REL");
        Assert.assertEquals("", builder.toString());
    }

    @Test
    public void appendPropertyToMarkdownIfSetAppendsPresentValue() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "Present", "yes");
        Assert.assertTrue(builder.toString().contains("Present"));
        Assert.assertTrue(builder.toString().contains("yes"));
    }

    @Test
    public void appendPropertyToMarkdownIfSetAppendsNonStringValue() {
        StringBuilder builder = new StringBuilder();
        AndroidUtils.appendPropertyToMarkdownIfSet(builder, "Number", 42);
        Assert.assertTrue(builder.toString().contains("Number"));
        Assert.assertTrue(builder.toString().contains("42"));
    }

    @Test
    public void getCurrentTimeStampMatchesUtcSecondsFormat() {
        Assert.assertTrue(UTC_SECONDS.matcher(AndroidUtils.getCurrentTimeStamp()).matches());
    }

    @Test
    public void getCurrentMilliSecondUTCTimeStampMatchesUtcMillisFormat() {
        Assert.assertTrue(UTC_MILLIS.matcher(AndroidUtils.getCurrentMilliSecondUTCTimeStamp()).matches());
    }

    @Test
    public void getCurrentMilliSecondLocalTimeStampMatchesLocalMillisFormat() {
        Assert.assertTrue(LOCAL_MILLIS.matcher(AndroidUtils.getCurrentMilliSecondLocalTimeStamp()).matches());
    }

    @Test
    public void getSystemPropertiesReturnsNonNullProperties() {
        Properties properties = AndroidUtils.getSystemProperties();
        Assert.assertNotNull(properties);
    }
}
