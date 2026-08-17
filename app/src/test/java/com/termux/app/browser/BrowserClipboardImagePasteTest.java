package com.termux.app.browser;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BrowserClipboardImagePasteTest {

    @Test
    public void extractImageMimeTypeReturnsNullForNullClipData() {
        Assert.assertNull(BrowserClipboardImagePaste.extractImageMimeType(null));
    }

    @Test
    public void extractImageMimeTypeReturnsNullWhenClipDataHasNoImageMimeType() {
        ClipData clipData = new ClipData(
            new ClipDescription("text", new String[]{"text/plain"}),
            new ClipData.Item("hello"));
        Assert.assertNull(BrowserClipboardImagePaste.extractImageMimeType(clipData));
    }

    @Test
    public void extractImageMimeTypeReturnsImageMimeTypeFromClipData() {
        ClipData clipData = new ClipData(
            new ClipDescription("image", new String[]{"image/png"}),
            new ClipData.Item(android.net.Uri.parse("content://images/1")));
        Assert.assertEquals("image/png", BrowserClipboardImagePaste.extractImageMimeType(clipData));
    }

    @Test
    public void extractImageMimeTypeReturnsFirstImageMimeTypeWhenMultiplePresent() {
        ClipData clipData = new ClipData(
            new ClipDescription("image", new String[]{"text/plain", "image/jpeg"}),
            new ClipData.Item("text"));
        Assert.assertEquals("image/jpeg", BrowserClipboardImagePaste.extractImageMimeType(clipData));
    }

    @Test
    public void extractImageMimeTypeReturnsNullForEmptyMimeTypeList() {
        ClipData clipData = new ClipData(
            new ClipDescription("empty", new String[0]),
            new ClipData.Item("x"));
        Assert.assertNull(BrowserClipboardImagePaste.extractImageMimeType(clipData));
    }

    @Test
    public void buildPasteScriptContainsTheProvidedDataUrl() {
        String dataUrl = "data:image/png;base64,iVBORw0KGgoAAAA==";
        String script = BrowserClipboardImagePaste.buildPasteScript(dataUrl);
        Assert.assertTrue("script must embed the data URL so the JS can decode it",
            script.contains(dataUrl));
    }

    @Test
    public void buildPasteScriptCreatesDataTransferWithFile() {
        String script = BrowserClipboardImagePaste.buildPasteScript("data:image/png;base64,AA==");
        Assert.assertTrue("DataTransfer must be constructed to carry the image as a file item",
            script.contains("new DataTransfer()"));
        Assert.assertTrue("File must be constructed from the decoded image bytes",
            script.contains("new File("));
        Assert.assertTrue("file must be added to the DataTransfer items",
            script.contains("dt.items.add("));
    }

    @Test
    public void buildPasteScriptDispatchesPasteEvent() {
        String script = BrowserClipboardImagePaste.buildPasteScript("data:image/png;base64,AA==");
        Assert.assertTrue("ClipboardEvent of type paste must be dispatched to the focused element",
            script.contains("new ClipboardEvent('paste'"));
        Assert.assertTrue("the event must be dispatched to the active element or document body",
            script.contains("dispatchEvent(ev)"));
    }

    @Test
    public void buildPasteScriptSetsClipboardDataOnPasteEvent() {
        String script = BrowserClipboardImagePaste.buildPasteScript("data:image/png;base64,AA==");
        Assert.assertTrue("clipboardData carrying the image must be attached to the paste event",
            script.contains("clipboardData:dt"));
    }

    @Test
    public void pasteIfImageInClipboardReturnsFalseWhenClipboardIsEmpty() {
        Context context = RuntimeEnvironment.getApplication();
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.clearPrimaryClip();
        boolean result = BrowserClipboardImagePaste.pasteIfImageInClipboard(context,
            new android.webkit.WebView(context));
        Assert.assertFalse("no paste should be attempted when the clipboard holds no image",
            result);
    }

    @Test
    public void pasteIfImageInClipboardReturnsFalseWhenClipboardHoldsOnlyText() {
        Context context = RuntimeEnvironment.getApplication();
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Assert.assertNotNull(cm);
        cm.setPrimaryClip(ClipData.newPlainText("label", "plain text content"));
        boolean result = BrowserClipboardImagePaste.pasteIfImageInClipboard(context,
            new android.webkit.WebView(context));
        Assert.assertFalse("text-only clipboard must not trigger the image paste path",
            result);
    }

    @Test
    public void pasteIfImageInClipboardReturnsTrueWhenClipboardHoldsImage() {
        Context context = RuntimeEnvironment.getApplication();
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Assert.assertNotNull(cm);
        ClipData imageClip = new ClipData(
            new ClipDescription("image", new String[]{"image/png"}),
            new ClipData.Item(android.net.Uri.parse("content://media/external/images/1")));
        cm.setPrimaryClip(imageClip);
        boolean result = BrowserClipboardImagePaste.pasteIfImageInClipboard(context,
            new android.webkit.WebView(context));
        Assert.assertTrue("an image in the clipboard must trigger the paste injection path",
            result);
    }
}
