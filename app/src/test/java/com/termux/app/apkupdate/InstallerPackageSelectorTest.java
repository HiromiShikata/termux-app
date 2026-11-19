package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InstallerPackageSelectorTest {

    private static final String GOOGLE_INSTALLER = "com.google.android.packageinstaller";
    private static final String AOSP_INSTALLER = "com.android.packageinstaller";

    private static final class SelectionCase {
        final List<String> resolvingPackageNames;
        final String expected;

        SelectionCase(List<String> resolvingPackageNames, String expected) {
            this.resolvingPackageNames = resolvingPackageNames;
            this.expected = expected;
        }
    }

    @Test
    public void selectsInstallerPackageByPreferenceOrder() {
        SelectionCase[] cases = {
            new SelectionCase(Arrays.asList(AOSP_INSTALLER), AOSP_INSTALLER),
            new SelectionCase(Arrays.asList(GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(AOSP_INSTALLER, GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(GOOGLE_INSTALLER, AOSP_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList("com.termux", "com.example.files", AOSP_INSTALLER), AOSP_INSTALLER),
            new SelectionCase(Arrays.asList(AOSP_INSTALLER, "com.termux", "com.example.files"), AOSP_INSTALLER),
            new SelectionCase(Arrays.asList("com.termux", "com.example.files", GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(GOOGLE_INSTALLER, "com.termux", "com.example.files"), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList("com.termux", "com.example.files"), null),
            new SelectionCase(Collections.<String>emptyList(), null),
        };
        for (SelectionCase testCase : cases) {
            Assert.assertEquals(testCase.resolvingPackageNames.toString(), testCase.expected,
                InstallerPackageSelector.select(testCase.resolvingPackageNames));
        }
    }
}
