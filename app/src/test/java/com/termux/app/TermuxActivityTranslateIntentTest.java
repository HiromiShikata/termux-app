package com.termux.app;

import android.content.Intent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityTranslateIntentTest {

    @Test
    public void testGoogleTranslateIntentTargetsGoogleTranslatePackageWithSelectedText() {
        String selectedText = "hello world";

        Intent intent = TermuxActivity.createGoogleTranslateProcessTextIntent(selectedText);

        Assert.assertEquals(Intent.ACTION_PROCESS_TEXT, intent.getAction());
        Assert.assertEquals("com.google.android.apps.translate", intent.getPackage());
        Assert.assertEquals(selectedText, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT));
        Assert.assertTrue(intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false));
        Assert.assertEquals("text/plain", intent.getType());
    }

    @Test
    public void testProcessTextIntentHasNoTargetPackageForChooserFallback() {
        Intent intent = TermuxActivity.createProcessTextIntent("bonjour");

        Assert.assertEquals(Intent.ACTION_PROCESS_TEXT, intent.getAction());
        Assert.assertNull(intent.getPackage());
        Assert.assertEquals("bonjour", intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT));
    }
}
