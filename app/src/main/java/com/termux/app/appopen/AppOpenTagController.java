package com.termux.app.appopen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class AppOpenTagController {

    public interface AppLauncher {
        void launchApp(@NonNull String packageId);
    }

    private final TermuxAppSharedPreferences mPreferences;

    private final Map<String, AppOpenTagScanner> mScannerBySessionName = new HashMap<>();

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

    public void onSessionTextChanged(String sessionName, String screenText,
                                     boolean outputNotYetSeenByTheOwner) {
        if (sessionName == null) return;
        if (!isAutoOpenEnabled()) return;

        AppLauncher appLauncher = mAppLauncher;
        if (appLauncher == null) return;

        AppOpenTagScanner scanner = mScannerBySessionName.get(sessionName);
        if (scanner == null) {
            scanner = new AppOpenTagScanner();
            mScannerBySessionName.put(sessionName, scanner);
            if (!outputNotYetSeenByTheOwner) {
                scanner.rememberWithoutLaunching(screenText);
                return;
            }
        }
        for (String packageId : scanner.packageIdsToLaunch(screenText)) {
            appLauncher.launchApp(packageId);
        }
    }

    public void forgetSessionsOtherThan(@NonNull Set<String> sessionNamesToKeep) {
        mScannerBySessionName.keySet().retainAll(sessionNamesToKeep);
    }
}
