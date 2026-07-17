package com.termux.app.apkupdate;

import android.content.pm.PackageInstaller;

import com.termux.shared.logger.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Requests that a {@link PackageInstaller.Session} be allowed to install an APK whose versionCode is
 * lower than the currently-installed one. Android exposes no public API for this, so the request is
 * applied through the hidden {@code SessionParams.setRequestDowngrade(boolean)} method, falling back
 * to setting the hidden {@code installFlags} field directly. The package manager honors the request
 * only for a debuggable target package (the builds published on the releases page are debug builds),
 * which is why an in-app revert to an older build is possible for this app while the plain
 * {@code ACTION_VIEW} installer intent is refused with "cannot be installed".
 */
public final class DowngradeRequestApplier {

    private static final String LOG_TAG = "DowngradeRequestApplier";

    private static final int INSTALL_REQUEST_DOWNGRADE_FLAG = 0x00000080;

    public boolean applyTo(PackageInstaller.SessionParams sessionParams) {
        if (applyViaSetter(sessionParams)) {
            return true;
        }
        return applyViaInstallFlagsField(sessionParams);
    }

    private boolean applyViaSetter(PackageInstaller.SessionParams sessionParams) {
        try {
            Method setter = PackageInstaller.SessionParams.class.getMethod("setRequestDowngrade", boolean.class);
            setter.invoke(sessionParams, true);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Logger.logVerbose(LOG_TAG, "setRequestDowngrade unavailable: " + messageOf(exception));
            return false;
        }
    }

    private boolean applyViaInstallFlagsField(PackageInstaller.SessionParams sessionParams) {
        try {
            Field installFlagsField = PackageInstaller.SessionParams.class.getDeclaredField("installFlags");
            installFlagsField.setAccessible(true);
            int currentFlags = installFlagsField.getInt(sessionParams);
            installFlagsField.setInt(sessionParams, currentFlags | INSTALL_REQUEST_DOWNGRADE_FLAG);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Logger.logError(LOG_TAG, "Failed to request install downgrade: " + messageOf(exception));
            return false;
        }
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage() : exception.toString();
    }
}
