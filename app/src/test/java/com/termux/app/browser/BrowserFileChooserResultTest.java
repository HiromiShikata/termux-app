package com.termux.app.browser;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserFileChooserResultTest {

    @Test
    public void parseReturnsNullWhenResultCodeIsNotOk() {
        Intent data = new Intent();
        data.setData(Uri.parse("content://files/1"));
        Assert.assertNull(BrowserFileChooserResult.parse(Activity.RESULT_CANCELED, data));
    }

    @Test
    public void parseReturnsNullWhenDataIsNull() {
        Assert.assertNull(BrowserFileChooserResult.parse(Activity.RESULT_OK, null));
    }

    @Test
    public void parseReturnsNullWhenDataHasNoUri() {
        Assert.assertNull(BrowserFileChooserResult.parse(Activity.RESULT_OK, new Intent()));
    }

    @Test
    public void parseReturnsSingleUriFromGetData() {
        Uri uri = Uri.parse("content://files/photo.png");
        Intent data = new Intent();
        data.setData(uri);
        Uri[] result = BrowserFileChooserResult.parse(Activity.RESULT_OK, data);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals(uri, result[0]);
    }

    @Test
    public void parseReturnsMultipleUrisFromClipData() {
        Uri first = Uri.parse("content://files/a.txt");
        Uri second = Uri.parse("content://files/b.txt");
        ClipData clipData = new ClipData(
            new ClipDescription("files", new String[]{"*/*"}),
            new ClipData.Item(first));
        clipData.addItem(new ClipData.Item(second));
        Intent data = new Intent();
        data.setClipData(clipData);
        Uri[] result = BrowserFileChooserResult.parse(Activity.RESULT_OK, data);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.length);
        Assert.assertEquals(first, result[0]);
        Assert.assertEquals(second, result[1]);
    }

    @Test
    public void parseDoesNotDuplicateGetDataAlreadyInClipData() {
        Uri uri = Uri.parse("content://files/only.txt");
        ClipData clipData = new ClipData(
            new ClipDescription("files", new String[]{"*/*"}),
            new ClipData.Item(uri));
        Intent data = new Intent();
        data.setClipData(clipData);
        data.setData(uri);
        Uri[] result = BrowserFileChooserResult.parse(Activity.RESULT_OK, data);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals(uri, result[0]);
    }

    @Test
    public void buildIntentUsesGetContentActionWithOpenableCategory() {
        Intent intent = BrowserFileChooserResult.buildIntent(false, null);
        Assert.assertEquals(Intent.ACTION_GET_CONTENT, intent.getAction());
        Assert.assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE));
        Assert.assertEquals("*/*", intent.getType());
        Assert.assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
    }

    @Test
    public void buildIntentEnablesMultipleSelectionWhenRequested() {
        Intent intent = BrowserFileChooserResult.buildIntent(true, null);
        Assert.assertTrue(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false));
    }

    @Test
    public void buildIntentNarrowsTypeToSingleMimeType() {
        Intent intent = BrowserFileChooserResult.buildIntent(false, new String[]{"image/png"});
        Assert.assertEquals("image/png", intent.getType());
        String[] extra = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
        Assert.assertNotNull(extra);
        Assert.assertArrayEquals(new String[]{"image/png"}, extra);
    }

    @Test
    public void buildIntentKeepsWildcardTypeForMultipleMimeTypes() {
        Intent intent = BrowserFileChooserResult.buildIntent(false, new String[]{"image/png", "application/pdf"});
        Assert.assertEquals("*/*", intent.getType());
        String[] extra = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);
        Assert.assertNotNull(extra);
        Assert.assertArrayEquals(new String[]{"image/png", "application/pdf"}, extra);
    }

    @Test
    public void resolveExtraMimeTypesSplitsCommaSeparatedTokens() {
        String[] resolved = BrowserFileChooserResult.resolveExtraMimeTypes(new String[]{"image/png,application/pdf"});
        Assert.assertArrayEquals(new String[]{"image/png", "application/pdf"}, resolved);
    }

    @Test
    public void resolveExtraMimeTypesIgnoresFileExtensionsAndBlanks() {
        String[] resolved = BrowserFileChooserResult.resolveExtraMimeTypes(new String[]{".png", "", "  ", "image/jpeg"});
        Assert.assertArrayEquals(new String[]{"image/jpeg"}, resolved);
    }

    @Test
    public void resolveExtraMimeTypesDeduplicates() {
        String[] resolved = BrowserFileChooserResult.resolveExtraMimeTypes(new String[]{"image/png", "image/png"});
        Assert.assertArrayEquals(new String[]{"image/png"}, resolved);
    }

    @Test
    public void resolveExtraMimeTypesReturnsEmptyForNull() {
        Assert.assertEquals(0, BrowserFileChooserResult.resolveExtraMimeTypes(null).length);
    }

    @Test
    public void resolveMimeTypeFallsBackToWildcardWhenNoMimeTypes() {
        Assert.assertEquals("*/*", BrowserFileChooserResult.resolveMimeType(new String[]{".png"}));
    }
}
