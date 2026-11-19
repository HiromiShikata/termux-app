package com.termux.app.apkupdate;

public enum ApkUpdateTapDestination {
    INSTALL_CONFIRMATION,
    UNKNOWN_APPS_SETTINGS;

    public static ApkUpdateTapDestination forInstallPermission(boolean canRequestPackageInstalls) {
        return canRequestPackageInstalls ? INSTALL_CONFIRMATION : UNKNOWN_APPS_SETTINGS;
    }
}
