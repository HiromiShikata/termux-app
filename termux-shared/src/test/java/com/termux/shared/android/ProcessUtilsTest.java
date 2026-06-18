package com.termux.shared.android;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivityManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ProcessUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getAppProcessNameForPidReturnsNullForNegativePid() {
        Assert.assertNull(ProcessUtils.getAppProcessNameForPid(context(), -1));
    }

    @Test
    public void getAppProcessNameForPidReturnsNullForUnknownPid() {
        Assert.assertNull(ProcessUtils.getAppProcessNameForPid(context(), 999999));
    }

    @Test
    public void getAppProcessNameForPidReturnsProcessNameForRegisteredPid() {
        ActivityManager activityManager =
            (ActivityManager) context().getSystemService(Context.ACTIVITY_SERVICE);
        ShadowActivityManager shadowActivityManager =
            org.robolectric.Shadows.shadowOf(activityManager);

        ActivityManager.RunningAppProcessInfo procInfo =
            new ActivityManager.RunningAppProcessInfo("com.termux.test.process", Process.myPid(), new String[0]);
        shadowActivityManager.setProcesses(Collections.singletonList(procInfo));

        Assert.assertEquals("com.termux.test.process",
            ProcessUtils.getAppProcessNameForPid(context(), Process.myPid()));
    }
}
