package com.termux.app.apkupdate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ApkUpdateInstallIntentTest {

    private static final String GOOGLE_INSTALLER = "com.google.android.packageinstaller";
    private static final String VENDOR_INSTALLER = "com.samsung.android.packageinstaller";
    private static final String UNKNOWN_FIRST = "com.example.first";
    private static final String UNKNOWN_SECOND = "com.example.second";

    private static final class InstallIntentCase {
        final String name;
        final List<String> resolvingPackageNames;
        final String expectedPackage;

        InstallIntentCase(String name, List<String> resolvingPackageNames, String expectedPackage) {
            this.name = name;
            this.resolvingPackageNames = resolvingPackageNames;
            this.expectedPackage = expectedPackage;
        }
    }

    private static ResolveInfo resolveInfoFor(String packageName) {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.name = packageName + ".InstallActivity";
        activityInfo.applicationInfo = new ApplicationInfo();
        activityInfo.applicationInfo.packageName = packageName;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        return resolveInfo;
    }

    @Test
    public void installConfirmationIntentTargetsSelectedInstallerPackage() {
        Context context = RuntimeEnvironment.getApplication();
        String ownPackage = context.getPackageName();
        InstallIntentCase[] cases = {
            new InstallIntentCase("google installer resolves beside own package",
                Arrays.asList(GOOGLE_INSTALLER, ownPackage), GOOGLE_INSTALLER),
            new InstallIntentCase("only own package resolves leaves package unset",
                Arrays.asList(ownPackage), null),
            new InstallIntentCase("vendor installer resolves beside own package",
                Arrays.asList(VENDOR_INSTALLER, ownPackage), VENDOR_INSTALLER),
            new InstallIntentCase("two unknown packages resolve leaves package unset",
                Arrays.asList(UNKNOWN_FIRST, UNKNOWN_SECOND), null),
        };
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(context.getPackageManager());
        ApkInstaller apkInstaller = new ApkInstaller(context);
        for (int index = 0; index < cases.length; index++) {
            InstallIntentCase testCase = cases[index];
            File apkFile = new File(context.getCacheDir(), "apkupdate/" + index + ".apk");
            Intent installIntent = apkInstaller.buildInstallIntent(apkFile);
            List<ResolveInfo> resolveInfos = new ArrayList<>();
            for (String packageName : testCase.resolvingPackageNames) {
                resolveInfos.add(resolveInfoFor(packageName));
            }
            shadowPackageManager.addResolveInfoForIntent(new Intent(installIntent), resolveInfos);

            Intent confirmationIntent = ApkUpdateNotifier.installConfirmationIntent(context, apkInstaller, apkFile);

            Assert.assertEquals(testCase.name, testCase.expectedPackage, confirmationIntent.getPackage());
        }
    }
}
