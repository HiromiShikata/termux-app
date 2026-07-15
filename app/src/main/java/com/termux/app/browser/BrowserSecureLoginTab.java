package com.termux.app.browser;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

import com.termux.shared.logger.Logger;

import java.util.Collections;

public final class BrowserSecureLoginTab {

    private static final String LOG_TAG = "BrowserSecureLoginTab";

    private static final String CHROME_PACKAGE_NAME = "com.android.chrome";

    private BrowserSecureLoginTab() {
    }

    public static BrowserSecureLoginTabLaunchMechanism resolveMechanism(@NonNull Context context) {
        boolean customTabsProviderAvailable = isCustomTabsProviderAvailable(context);
        return BrowserSecureLoginTabLaunchMechanism.resolve(
            isAuthTabApiAvailable(), customTabsProviderAvailable);
    }

    static boolean isAuthTabApiAvailable() {
        try {
            Class.forName("androidx.browser.auth.AuthTabIntent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean isCustomTabsProviderAvailable(@NonNull Context context) {
        try {
            String packageName = CustomTabsClient.getPackageName(
                context, Collections.singletonList(CHROME_PACKAGE_NAME));
            if (packageName != null) return true;
            return CustomTabsClient.getPackageName(context, Collections.emptyList()) != null;
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to resolve Custom Tabs provider", e);
            return false;
        }
    }

    public static void openInCustomTab(@NonNull Context context, @NonNull String url) {
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build();
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open url in secure login tab \"" + url + "\"", e);
        }
    }
}
