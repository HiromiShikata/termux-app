package com.termux.shared.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class PackageUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void getPackageInfoForPackageReturnsInfoForCurrentPackage() {
        Context context = context();
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(context);
        Assert.assertNotNull(packageInfo);
        Assert.assertEquals(context.getPackageName(), packageInfo.packageName);
    }

    @Test
    public void getApplicationInfoForPackageReturnsInfoForCurrentPackage() {
        Context context = context();
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(context, context.getPackageName());
        Assert.assertNotNull(applicationInfo);
        Assert.assertEquals(context.getPackageName(), applicationInfo.packageName);
    }

    @Test
    public void getApplicationInfoForPackageReturnsNullForUnknownPackage() {
        Assert.assertNull(PackageUtils.getApplicationInfoForPackage(context(), "non.existent.package.xyz"));
    }

    @Test
    public void getAppNameForPackageReturnsNonNull() {
        Assert.assertNotNull(PackageUtils.getAppNameForPackage(context()));
    }

    @Test
    public void getPackageNameForPackageFromContextMatchesContext() {
        Context context = context();
        Assert.assertEquals(context.getPackageName(), PackageUtils.getPackageNameForPackage(context));
    }

    @Test
    public void getPackageNameForPackageFromApplicationInfoMatchesField() {
        Context context = context();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Assert.assertEquals(applicationInfo.packageName, PackageUtils.getPackageNameForPackage(applicationInfo));
    }

    @Test
    public void getUidForPackageMatchesApplicationInfoUid() {
        Context context = context();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Assert.assertEquals(applicationInfo.uid, PackageUtils.getUidForPackage(applicationInfo));
        Assert.assertEquals(applicationInfo.uid, PackageUtils.getUidForPackage(context));
    }

    @Test
    public void getTargetSDKForPackageMatchesApplicationInfo() {
        Context context = context();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Assert.assertEquals(applicationInfo.targetSdkVersion, PackageUtils.getTargetSDKForPackage(applicationInfo));
    }

    @Test
    public void getBaseAPKPathForPackageMatchesApplicationInfo() {
        Context context = context();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Assert.assertEquals(applicationInfo.publicSourceDir, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
    }

    @Test
    public void isAppForPackageADebuggableBuildMatchesFlag() {
        Context context = context();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        boolean expected = (0 != (applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE));
        Assert.assertEquals(expected, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
    }

    @Test
    public void getApplicationInfoStaticIntFieldValueResolvesKnownField() {
        Integer value = PackageUtils.getApplicationInfoStaticIntFieldValue("FLAG_DEBUGGABLE");
        Assert.assertNotNull(value);
        Assert.assertEquals(ApplicationInfo.FLAG_DEBUGGABLE, value.intValue());
    }

    @Test
    public void getApplicationInfoStaticIntFieldValueReturnsNullForUnknownField() {
        Assert.assertNull(PackageUtils.getApplicationInfoStaticIntFieldValue("THIS_FIELD_DOES_NOT_EXIST"));
    }

    @Test
    public void isApplicationInfoPrivateFlagSetForPackageReturnsNullForUnknownFlag() {
        Context context = context();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Assert.assertNull(PackageUtils.isApplicationInfoPrivateFlagSetForPackage("THIS_PRIVATE_FLAG_DOES_NOT_EXIST", applicationInfo));
    }

    @Test
    public void getVersionCodeForPackageReturnsNullForNullPackageInfo() {
        Assert.assertNull(PackageUtils.getVersionCodeForPackage((PackageInfo) null));
    }

    @Test
    public void getVersionCodeForPackageReturnsValueForPackageInfo() {
        Context context = context();
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(context);
        Assert.assertNotNull(packageInfo);
        Assert.assertEquals(Integer.valueOf(packageInfo.versionCode), PackageUtils.getVersionCodeForPackage(packageInfo));
    }

    @Test
    public void getVersionNameForPackageReturnsNullForNullPackageInfo() {
        Assert.assertNull(PackageUtils.getVersionNameForPackage((PackageInfo) null));
    }
}
