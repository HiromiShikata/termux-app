package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InstallerPackageSelectorTest {

    private static final String GOOGLE_INSTALLER = "com.google.android.packageinstaller";
    private static final String AOSP_INSTALLER = "com.android.packageinstaller";
    private static final String VENDOR_INSTALLER = "com.samsung.android.packageinstaller";
    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String OTHER_PACKAGE = "com.example.files";

    private static final class SelectionCase {
        final List<String> resolvingPackageNames;
        final String expected;

        SelectionCase(List<String> resolvingPackageNames, String expected) {
            this.resolvingPackageNames = resolvingPackageNames;
            this.expected = expected;
        }
    }

    @Test
    public void selectsInstallerPackageByPreferenceOrderThenSingleVendorOrNull() {
        SelectionCase[] cases = {
            new SelectionCase(Arrays.asList(AOSP_INSTALLER), AOSP_INSTALLER),
            new SelectionCase(Arrays.asList(GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(AOSP_INSTALLER, GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(GOOGLE_INSTALLER, AOSP_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(TERMUX_PACKAGE, OTHER_PACKAGE, AOSP_INSTALLER), AOSP_INSTALLER),
            new SelectionCase(Arrays.asList(AOSP_INSTALLER, TERMUX_PACKAGE, OTHER_PACKAGE), AOSP_INSTALLER),
            new SelectionCase(Arrays.asList(TERMUX_PACKAGE, OTHER_PACKAGE, GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(GOOGLE_INSTALLER, TERMUX_PACKAGE, OTHER_PACKAGE), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(VENDOR_INSTALLER, GOOGLE_INSTALLER), GOOGLE_INSTALLER),
            new SelectionCase(Arrays.asList(TERMUX_PACKAGE, OTHER_PACKAGE, VENDOR_INSTALLER), null),
            new SelectionCase(Arrays.asList(VENDOR_INSTALLER), VENDOR_INSTALLER),
            new SelectionCase(Arrays.asList(VENDOR_INSTALLER, VENDOR_INSTALLER), VENDOR_INSTALLER),
            new SelectionCase(Arrays.asList(TERMUX_PACKAGE), TERMUX_PACKAGE),
            new SelectionCase(Arrays.asList(TERMUX_PACKAGE, OTHER_PACKAGE), null),
            new SelectionCase(Collections.<String>emptyList(), null),
        };
        for (SelectionCase testCase : cases) {
            Assert.assertEquals(testCase.resolvingPackageNames.toString(), testCase.expected,
                InstallerPackageSelector.select(testCase.resolvingPackageNames));
        }
    }
}
