package com.termux.shared.android;

import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class SELinuxUtilsTest {

    @Test
    public void androidOsSELinuxClassConstantHasExpectedName() {
        Assert.assertEquals("android.os.SELinux", SELinuxUtils.ANDROID_OS_SELINUX_CLASS);
    }

    @Test
    public void getContextReturnsNullWhenSELinuxClassUnavailable() {
        Assert.assertNull(SELinuxUtils.getContext());
    }

    @Test
    public void getPidContextReturnsNullWhenSELinuxClassUnavailable() {
        Assert.assertNull(SELinuxUtils.getPidContext(1));
    }

    @Test
    public void getFileContextReturnsNullWhenSELinuxClassUnavailable() {
        Assert.assertNull(SELinuxUtils.getFileContext("/"));
    }
}
