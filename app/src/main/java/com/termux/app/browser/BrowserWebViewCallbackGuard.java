package com.termux.app.browser;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;

public final class BrowserWebViewCallbackGuard {

    public interface GuardedAction {
        void run() throws Exception;
    }

    public interface GuardedBooleanAction {
        boolean run() throws Exception;
    }

    private static final String LOG_TAG = "BrowserWebViewCallbackGuard";

    private final String mLogTag;

    public BrowserWebViewCallbackGuard() {
        this(LOG_TAG);
    }

    public BrowserWebViewCallbackGuard(@NonNull String logTag) {
        this.mLogTag = logTag;
    }

    public void run(@NonNull String callbackName, @NonNull GuardedAction action) {
        try {
            action.run();
        } catch (Exception | Error throwable) {
            Logger.logStackTraceWithMessage(mLogTag,
                "Swallowed exception thrown in WebView callback " + callbackName
                    + " to prevent a JNI-rethrown app crash", throwable);
        }
    }

    public boolean runReturning(@NonNull String callbackName, boolean fallback,
                                @NonNull GuardedBooleanAction action) {
        try {
            return action.run();
        } catch (Exception | Error throwable) {
            Logger.logStackTraceWithMessage(mLogTag,
                "Swallowed exception thrown in WebView callback " + callbackName
                    + " to prevent a JNI-rethrown app crash", throwable);
            return fallback;
        }
    }
}
