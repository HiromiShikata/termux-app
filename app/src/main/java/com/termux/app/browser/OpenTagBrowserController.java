package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.HashMap;
import java.util.Map;

public final class OpenTagBrowserController {

    public interface UrlOpener {
        void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url);
    }

    private final TermuxAppSharedPreferences mPreferences;

    private final Map<String, OpenTagScanner> mScannerBySessionKey = new HashMap<>();

    private UrlOpener mUrlOpener;

    public OpenTagBrowserController(@NonNull TermuxAppSharedPreferences preferences, @Nullable UrlOpener urlOpener) {
        mPreferences = preferences;
        mUrlOpener = urlOpener;
    }

    public void setUrlOpener(@Nullable UrlOpener urlOpener) {
        mUrlOpener = urlOpener;
    }

    public boolean isAutoOpenEnabled() {
        return mPreferences.isOpenTagAutoOpenEnabled();
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;
        if (!isAutoOpenEnabled()) return;

        UrlOpener urlOpener = mUrlOpener;
        if (urlOpener == null) return;

        OpenTagScanner scanner = scannerForSession(sessionKey);
        for (String openUrl : scanner.urlsToOpen(screenText)) {
            urlOpener.openUrlInTabForSession(sessionKey, openUrl);
        }
    }

    public void forgetSession(String sessionKey) {
        if (sessionKey == null) return;
        mScannerBySessionKey.remove(sessionKey);
    }

    private OpenTagScanner scannerForSession(String sessionKey) {
        OpenTagScanner scanner = mScannerBySessionKey.get(sessionKey);
        if (scanner == null) {
            scanner = new OpenTagScanner();
            mScannerBySessionKey.put(sessionKey, scanner);
        }
        return scanner;
    }
}
