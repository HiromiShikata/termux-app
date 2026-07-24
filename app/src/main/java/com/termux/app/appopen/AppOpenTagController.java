package com.termux.app.appopen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.HashMap;
import java.util.Map;

public final class AppOpenTagController {

    public interface AppLauncher {
        void launchApp(@NonNull String packageId);
    }

    private final TermuxAppSharedPreferences mPreferences;

    private final Map<String, AppOpenTagScanner> mScannerBySessionKey = new HashMap<>();

    private AppLauncher mAppLauncher;

    public AppOpenTagController(@NonNull TermuxAppSharedPreferences preferences, @Nullable AppLauncher appLauncher) {
        mPreferences = preferences;
        mAppLauncher = appLauncher;
    }

    public void setAppLauncher(@Nullable AppLauncher appLauncher) {
        mAppLauncher = appLauncher;
    }

    public boolean isAutoOpenEnabled() {
        return mPreferences.isOpenTagAutoOpenEnabled();
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;
        if (!isAutoOpenEnabled()) return;

        AppLauncher appLauncher = mAppLauncher;
        if (appLauncher == null) return;

        AppOpenTagScanner scanner = scannerForSession(sessionKey);
        for (String packageId : scanner.packageIdsToLaunch(screenText)) {
            appLauncher.launchApp(packageId);
        }
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private AppOpenTagScanner scannerForSession(String sessionKey) {
        AppOpenTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new AppOpenTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }
}
