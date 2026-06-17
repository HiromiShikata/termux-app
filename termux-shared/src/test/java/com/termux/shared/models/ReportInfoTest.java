package com.termux.shared.models;

import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ReportInfoTest {

    private ReportInfo newInfo() {
        return new ReportInfo("com.example.ACTION", "com.example.sender", "Report Title");
    }

    @Test
    public void constructorStoresActionSenderAndTitle() {
        ReportInfo info = newInfo();
        Assert.assertEquals("com.example.ACTION", info.userAction);
        Assert.assertEquals("com.example.sender", info.sender);
        Assert.assertEquals("Report Title", info.reportTitle);
    }

    @Test
    public void constructorSetsNonNullTimestamp() {
        ReportInfo info = newInfo();
        Assert.assertNotNull(info.reportTimestamp);
        Assert.assertFalse(info.reportTimestamp.isEmpty());
    }

    @Test
    public void defaultsAreUnset() {
        ReportInfo info = newInfo();
        Assert.assertNull(info.reportStringPrefix);
        Assert.assertNull(info.reportString);
        Assert.assertNull(info.reportStringSuffix);
        Assert.assertFalse(info.addReportInfoHeaderToMarkdown);
        Assert.assertNull(info.reportSaveFileLabel);
        Assert.assertNull(info.reportSaveFilePath);
    }

    @Test
    public void settersStoreValues() {
        ReportInfo info = newInfo();
        info.setReportStringPrefix("prefix");
        info.setReportString("body");
        info.setReportStringSuffix("suffix");
        info.setAddReportInfoHeaderToMarkdown(true);

        Assert.assertEquals("prefix", info.reportStringPrefix);
        Assert.assertEquals("body", info.reportString);
        Assert.assertEquals("suffix", info.reportStringSuffix);
        Assert.assertTrue(info.addReportInfoHeaderToMarkdown);
    }

    @Test
    public void setReportSaveFileLabelAndPathStoresBoth() {
        ReportInfo info = newInfo();
        info.setReportSaveFileLabelAndPath("label", "/path/to/file");
        Assert.assertEquals("label", info.reportSaveFileLabel);
        Assert.assertEquals("/path/to/file", info.reportSaveFilePath);
    }

    @Test
    public void setReportSaveFileLabelAndPathIndividualSetters() {
        ReportInfo info = newInfo();
        info.setReportSaveFileLabel("only-label");
        info.setReportSaveFilePath("/only/path");
        Assert.assertEquals("only-label", info.reportSaveFileLabel);
        Assert.assertEquals("/only/path", info.reportSaveFilePath);
    }

    @Test
    public void markdownStringForNullReportInfoReturnsNullLiteral() {
        Assert.assertEquals("null", ReportInfo.getReportInfoMarkdownString(null));
    }

    @Test
    public void markdownStringWithoutHeaderReturnsReportStringOnly() {
        ReportInfo info = newInfo();
        info.setReportString("plain report body");
        Assert.assertEquals("plain report body", ReportInfo.getReportInfoMarkdownString(info));
    }

    @Test
    public void markdownStringWithHeaderIncludesReportInfoSection() {
        ReportInfo info = newInfo();
        info.setReportString("the body");
        info.setAddReportInfoHeaderToMarkdown(true);

        String markdown = ReportInfo.getReportInfoMarkdownString(info);
        Assert.assertTrue(markdown.contains("## Report Info"));
        Assert.assertTrue(markdown.contains("User Action"));
        Assert.assertTrue(markdown.contains("Sender"));
        Assert.assertTrue(markdown.contains("Report Timestamp"));
        Assert.assertTrue(markdown.contains("the body"));
    }
}
