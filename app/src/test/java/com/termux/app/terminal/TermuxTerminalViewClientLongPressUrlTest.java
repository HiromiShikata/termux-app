package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TermuxTerminalViewClientLongPressUrlTest {

    @Test
    public void testHyperlinkUriIsPreferredOverWordUrl() {
        String selected = TermuxTerminalViewClient.selectLongPressUrl(
            "https://hyperlink.example.com", "https://word.example.com");

        Assert.assertEquals("https://hyperlink.example.com", selected);
    }

    @Test
    public void testFallsBackToUrlExtractedFromWordWhenNoHyperlink() {
        String selected = TermuxTerminalViewClient.selectLongPressUrl(
            null, "see https://word.example.com now");

        Assert.assertEquals("https://word.example.com", selected);
    }

    @Test
    public void testFallsBackToWordUrlWhenHyperlinkIsEmpty() {
        String selected = TermuxTerminalViewClient.selectLongPressUrl(
            "", "https://word.example.com");

        Assert.assertEquals("https://word.example.com", selected);
    }

    @Test
    public void testReturnsNullWhenNeitherHyperlinkNorWordUrlPresent() {
        String selected = TermuxTerminalViewClient.selectLongPressUrl(null, "just plain text");

        Assert.assertNull(selected);
    }
}
