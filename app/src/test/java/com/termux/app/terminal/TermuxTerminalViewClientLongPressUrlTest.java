package com.termux.app.terminal;

import android.content.ClipboardManager;
import android.content.Context;

import com.termux.shared.interact.ShareUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

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

    @Test
    public void testLongPressedUrlMenuItemsShownWhenUrlPresent() {
        Assert.assertTrue(
            TermuxTerminalViewClient.shouldShowLongPressedUrlMenuItems("https://example.com"));
    }

    @Test
    public void testLongPressedUrlMenuItemsHiddenWhenUrlNull() {
        Assert.assertFalse(TermuxTerminalViewClient.shouldShowLongPressedUrlMenuItems(null));
    }

    @Test
    public void testLongPressedUrlMenuItemsHiddenWhenUrlEmpty() {
        Assert.assertFalse(TermuxTerminalViewClient.shouldShowLongPressedUrlMenuItems(""));
    }

    @Test
    public void testCopyUrlActionPutsUrlOnClipboard() {
        Context context = RuntimeEnvironment.getApplication();
        String url = "https://copy.example.com/page";

        ShareUtils.copyTextToClipboard(context, url, null);

        ClipboardManager clipboardManager =
            (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Assert.assertNotNull(clipboardManager.getPrimaryClip());
        Assert.assertEquals(url,
            clipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
    }
}
