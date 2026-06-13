package com.termux.app.terminal;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.List;

public final class SessionDefinitionEntriesProvider {

    public interface OnEntriesLoadedListener {
        void onEntriesLoaded();
    }

    private static final String LOG_TAG = "SessionDefinitionEntriesProvider";

    private final SessionDefinitionLoader mLoader;

    private List<SessionDefinitionEntry> mEntries = Collections.emptyList();

    private boolean mLoaded;

    private boolean mLoading;

    public SessionDefinitionEntriesProvider(@NonNull SessionDefinitionLoader loader) {
        this.mLoader = loader;
    }

    @NonNull
    public List<SessionDefinitionEntry> getEntries() {
        return mEntries;
    }

    public void load(@NonNull String baseUrl, @NonNull OnEntriesLoadedListener listener) {
        if (mLoaded || mLoading || baseUrl.isEmpty()) {
            return;
        }
        mLoading = true;
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> entries = mLoader.load(baseUrl);
                mainThreadHandler.post(() -> {
                    mEntries = entries;
                    mLoaded = true;
                    mLoading = false;
                    listener.onEntriesLoaded();
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load session definition entries from " + baseUrl, exception);
                mainThreadHandler.post(() -> mLoading = false);
            }
        }).start();
    }
}
