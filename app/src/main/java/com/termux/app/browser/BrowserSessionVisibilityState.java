package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class BrowserSessionVisibilityState {

    public interface PersistedNamesListener {
        void onPersistedOpenSessionNamesChanged(@NonNull Set<String> openSessionNames);
    }

    private final Set<String> mBrowserVisibleSessionHandles = new LinkedHashSet<>();
    private final Set<String> mBrowserOpenSessionNames = new LinkedHashSet<>();

    @Nullable
    private PersistedNamesListener mPersistedNamesListener;

    public void setPersistedNamesListener(@Nullable PersistedNamesListener listener) {
        mPersistedNamesListener = listener;
    }

    public void setPersistedOpenSessionNames(@NonNull Set<String> openSessionNames) {
        mBrowserOpenSessionNames.clear();
        for (String sessionName : openSessionNames) {
            if (sessionName != null && !sessionName.isEmpty()) mBrowserOpenSessionNames.add(sessionName);
        }
    }

    public boolean wasBrowserOpenForSessionName(@Nullable String sessionName) {
        return sessionName != null && mBrowserOpenSessionNames.contains(sessionName);
    }

    public void setBrowserVisible(@Nullable String sessionHandle, boolean browserVisible) {
        if (sessionHandle == null) return;
        if (browserVisible) {
            mBrowserVisibleSessionHandles.add(sessionHandle);
        } else {
            mBrowserVisibleSessionHandles.remove(sessionHandle);
        }
    }

    public void setBrowserVisible(
        @Nullable String sessionHandle, @Nullable String sessionName, boolean browserVisible) {
        setBrowserVisible(sessionHandle, browserVisible);
        updatePersistedName(sessionName, browserVisible);
    }

    public boolean wasBrowserVisible(@Nullable String sessionHandle) {
        return sessionHandle != null && mBrowserVisibleSessionHandles.contains(sessionHandle);
    }

    public void clearSession(@Nullable String sessionHandle) {
        if (sessionHandle == null) return;
        mBrowserVisibleSessionHandles.remove(sessionHandle);
    }

    public void moveSession(@Nullable String fromSessionHandle, @Nullable String toSessionHandle) {
        if (fromSessionHandle == null || toSessionHandle == null) return;
        if (!mBrowserVisibleSessionHandles.remove(fromSessionHandle)) return;
        mBrowserVisibleSessionHandles.add(toSessionHandle);
    }

    public void clearSession(@Nullable String sessionHandle, @Nullable String sessionName) {
        clearSession(sessionHandle);
        updatePersistedName(sessionName, false);
    }

    public boolean shouldRestoreBrowserOnSessionChange(
        @Nullable String targetSessionHandle, boolean targetSessionHasActiveTab) {
        return targetSessionHasActiveTab && wasBrowserVisible(targetSessionHandle);
    }

    private void updatePersistedName(@Nullable String sessionName, boolean browserVisible) {
        if (sessionName == null || sessionName.isEmpty()) return;
        boolean changed;
        if (browserVisible) {
            changed = mBrowserOpenSessionNames.add(sessionName);
        } else {
            changed = mBrowserOpenSessionNames.remove(sessionName);
        }
        if (changed && mPersistedNamesListener != null) {
            mPersistedNamesListener.onPersistedOpenSessionNamesChanged(
                new LinkedHashSet<>(mBrowserOpenSessionNames));
        }
    }
}
