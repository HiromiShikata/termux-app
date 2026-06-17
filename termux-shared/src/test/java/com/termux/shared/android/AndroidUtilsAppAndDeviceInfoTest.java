package com.termux.shared.android;

import android.content.Context;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class AndroidUtilsAppAndDeviceInfoTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getAppInfoMarkdownStringForContextReturnsNonNullString() {
        String markdown = AndroidUtils.getAppInfoMarkdownString(context());
        Assert.assertNotNull(markdown);
    }

    @Test
    public void getAppInfoMarkdownStringForPackageIncludesPackageDetails() {
        String markdown = AndroidUtils.getAppInfoMarkdownString(context(), context().getPackageName());
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("PACKAGE_NAME"));
        Assert.assertTrue(markdown.contains("VERSION_NAME"));
    }

    @Test
    public void getAppInfoMarkdownStringForMissingPackageReturnsNull() {
        Assert.assertNull(
            AndroidUtils.getAppInfoMarkdownString(context(), "com.example.package.that.does.not.exist"));
    }

    @Test
    public void getDeviceInfoMarkdownStringContainsSoftwareAndHardwareSections() {
        String markdown = AndroidUtils.getDeviceInfoMarkdownString(context());
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.startsWith("## Device Info"));
        Assert.assertTrue(markdown.contains("### Software"));
        Assert.assertTrue(markdown.contains("### Hardware"));
        Assert.assertTrue(markdown.contains("SDK_INT"));
        Assert.assertTrue(markdown.contains("MANUFACTURER"));
    }

    @Test
    public void getDeviceInfoMarkdownStringWithPhantomFlagContainsSections() {
        String markdown = AndroidUtils.getDeviceInfoMarkdownString(context(), true);
        Assert.assertNotNull(markdown);
        Assert.assertTrue(markdown.contains("### Hardware"));
    }
}
