package com.termux.app.browser;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.HashMap;
import java.util.Map;

public final class OpenTagBrowserController {

    public interface UrlOpener {
        void openUrlInNewTab(@NonNull String url);
    }

    private final TermuxAppSharedPreferences mPreferences;

    private final UrlOpener mUrlOpener;

    private final Map<String, OpenTagScanner> mScannerBySessionKey = new HashMap<>();

    public OpenTagBrowserController(@NonNull TermuxAppSharedPreferences preferences, @NonNull UrlOpener urlOpener) {
        mPreferences = preferences;
        mUrlOpener = urlOpener;
    }

    public boolean isAutoOpenEnabled() {
        return mPreferences.isOpenTagAutoOpenEnabled();
    }

    public void onSessionTextChanged(String sessionKey, String screenText) {
        if (sessionKey == null) return;
        if (!isAutoOpenEnabled()) return;

        OpenTagScanner scanner = scannerForSession(sessionKey);
        String openUrl = scanner.newOpenUrl(screenText);
        if (openUrl == null) return;

        scanner.markOpened(openUrl);
        mUrlOpener.openUrlInNewTab(openUrl);
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
