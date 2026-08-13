package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class OpenTagBrowserController {

    public interface UrlOpener {
        void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url);
    }

    private final TermuxAppSharedPreferences mPreferences;

    private final Map<String, OpenTagScanner> mScannerBySessionName = new HashMap<>();

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

    public void onSessionTextChanged(String sessionName, String sessionKey, String screenText,
                                     boolean outputNotYetSeenByTheOwner) {
        if (sessionName == null) return;
        if (sessionKey == null) return;
        if (!isAutoOpenEnabled()) return;

        UrlOpener urlOpener = mUrlOpener;
        if (urlOpener == null) return;

        OpenTagScanner scanner = mScannerBySessionName.get(sessionName);
        if (scanner == null) {
            scanner = new OpenTagScanner();
            mScannerBySessionName.put(sessionName, scanner);
            if (!outputNotYetSeenByTheOwner) {
                scanner.rememberWithoutOpening(screenText);
                return;
            }
        }
        for (String openUrl : scanner.urlsToOpen(screenText)) {
            urlOpener.openUrlInTabForSession(sessionKey, openUrl);
        }
    }

    public void forgetSessionsOtherThan(@NonNull Set<String> sessionNamesToKeep) {
        mScannerBySessionName.keySet().retainAll(sessionNamesToKeep);
    }
}
