package com.termux.shared.android;

import android.content.Context;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class PermissionUtilsTest {

    private static final String INTERNET = "android.permission.INTERNET";

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void checkPermissionReturnsFalseForPermissionNotRequestedInManifest() {
        Assert.assertFalse(PermissionUtils.checkPermission(context(), INTERNET));
    }

    @Test
    public void checkPermissionsReturnsTrueForEmptyPermissionArray() {
        Assert.assertTrue(PermissionUtils.checkPermissions(context(), new String[]{}));
    }

    @Test
    public void getPermissionsNotRequestedListsRequestedPermissionMissingFromManifest() {
        List<String> notRequested = PermissionUtils.getPermissionsNotRequested(context(), new String[]{INTERNET});
        Assert.assertTrue(notRequested.contains(INTERNET));
    }

    @Test
    public void getPermissionsNotRequestedReturnsEmptyListForEmptyInput() {
        List<String> notRequested = PermissionUtils.getPermissionsNotRequested(context(), new String[]{});
        Assert.assertTrue(notRequested.isEmpty());
    }

    @Test
    public void isPermissionRequestedReturnsFalseWhenNotInManifest() {
        Assert.assertFalse(PermissionUtils.isPermissionRequested(context(), INTERNET));
    }

    @Test
    public void checkStoragePermissionReturnsFalseWhenNotGranted() {
        Assert.assertFalse(PermissionUtils.checkStoragePermission(context(), true));
        Assert.assertFalse(PermissionUtils.checkStoragePermission(context(), false));
    }

    @Test
    public void checkDisplayOverOtherAppsPermissionReturnsFalseByDefault() {
        Assert.assertFalse(PermissionUtils.checkDisplayOverOtherAppsPermission(context()));
    }

    @Test
    public void checkIfBatteryOptimizationsDisabledReturnsFalseByDefault() {
        Assert.assertFalse(PermissionUtils.checkIfBatteryOptimizationsDisabled(context()));
    }

    @Test
    public void validateDisplayOverOtherAppsPermissionForPreAndroid10ReturnsTrue() {
        Assert.assertTrue(PermissionUtils.validateDisplayOverOtherAppsPermissionForPostAndroid10(context(), false));
    }

    @Test
    public void requestPermissionConstantsAreStable() {
        Assert.assertEquals(1000, PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION);
        Assert.assertEquals(2000, PermissionUtils.REQUEST_DISABLE_BATTERY_OPTIMIZATIONS);
        Assert.assertEquals(2001, PermissionUtils.REQUEST_GRANT_DISPLAY_OVER_OTHER_APPS_PERMISSION);
    }
}
