package com.termux.shared.android;

import android.content.Context;
import android.os.Build;
import android.os.Process;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class UserUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getNameForUidFromPackageManagerReturnsNullForNegativeUid() {
        Assert.assertNull(UserUtils.getNameForUidFromPackageManager(context(), -1));
    }

    @Test
    public void getNameForUidFromLibcoreReturnsNullForNegativeUid() {
        Assert.assertNull(UserUtils.getNameForUidFromLibcore(-5));
    }

    @Test
    public void getNameForUidReturnsNullForNegativeUid() {
        Assert.assertNull(UserUtils.getNameForUid(context(), -1));
    }

    @Test
    public void getNameForUidFromPackageManagerReturnsNameWithoutUidSuffixForOwnUid() {
        String name = UserUtils.getNameForUidFromPackageManager(context(), Process.myUid());
        if (name != null) {
            Assert.assertFalse(name.endsWith(":" + Process.myUid()));
        }
    }
}
