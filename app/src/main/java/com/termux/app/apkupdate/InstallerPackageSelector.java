package com.termux.app.apkupdate;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class InstallerPackageSelector {

    private static final List<String> PREFERRED_INSTALLER_PACKAGE_NAMES = Arrays.asList(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    );

    private InstallerPackageSelector() {
    }

    static String select(List<String> resolvingPackageNames) {
        for (String preferredPackageName : PREFERRED_INSTALLER_PACKAGE_NAMES) {
            if (resolvingPackageNames.contains(preferredPackageName)) {
                return preferredPackageName;
            }
        }
        Set<String> distinctPackageNames = new LinkedHashSet<>(resolvingPackageNames);
        if (distinctPackageNames.size() == 1) {
            return distinctPackageNames.iterator().next();
        }
        return null;
    }
}
