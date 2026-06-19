package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;
import com.termux.app.sessiondefinition.HttpSessionDefinitionDocumentFetcher;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.app.sessiondefinition.SessionDefinitionParser;
import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.List;

public final class BrowserProjectNameResolver {

    private static final String LOG_TAG = "BrowserProjectNameResolver";

    private final TermuxActivity mActivity;

    private final SessionDefinitionLoader mLoader;

    private final SessionDefinitionEntryMatcher mMatcher = new SessionDefinitionEntryMatcher();

    private List<SessionDefinitionEntry> mCachedEntries = Collections.emptyList();

    public BrowserProjectNameResolver(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mLoader =
            new SessionDefinitionLoader(new HttpSessionDefinitionDocumentFetcher(), new SessionDefinitionParser());
    }

    public void loadEntriesForSession(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return;
        }

        if (!mCachedEntries.isEmpty()) {
            return;
        }

        String baseUrl = mActivity.getPreferences().getSessionDefinitionUrl().trim();
        if (baseUrl.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> entries = mLoader.load(baseUrl);
                mActivity.runOnUiThread(() -> mCachedEntries = entries);
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load project URLs from " + baseUrl, exception);
            }
        }).start();
    }

    @Nullable
    public String resolveProjectName(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty() || mCachedEntries.isEmpty()) {
            return null;
        }
        return mMatcher.findGroupLabelForSessionName(mCachedEntries, sessionName);
    }
}
