package com.termux.app.browser;

import android.os.Handler;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPasskeyBridge {

    public interface Host {

        void onPasskeyCeremonyDetected();

        void onLoginFormDetected();
    }

    private final Handler mMainHandler;

    private final Host mHost;

    private final BrowserPasskeyHintDebounce mPasskeyDebounce;

    private final BrowserPasskeyHintDebounce mLoginFormDebounce;

    private final Clock mClock;

    interface Clock {

        long nowMs();
    }

    public BrowserPasskeyBridge(@NonNull Handler mainHandler, @NonNull Host host) {
        this(mainHandler, host, new BrowserPasskeyHintDebounce(), new BrowserPasskeyHintDebounce(),
            System::currentTimeMillis);
    }

    BrowserPasskeyBridge(
            @NonNull Handler mainHandler,
            @NonNull Host host,
            @NonNull BrowserPasskeyHintDebounce passkeyDebounce,
            @NonNull BrowserPasskeyHintDebounce loginFormDebounce,
            @NonNull Clock clock) {
        this.mMainHandler = mainHandler;
        this.mHost = host;
        this.mPasskeyDebounce = passkeyDebounce;
        this.mLoginFormDebounce = loginFormDebounce;
        this.mClock = clock;
    }

    @JavascriptInterface
    public void onPasskeyCeremonyDetected(@Nullable String ignoredPageUrl) {
        mMainHandler.post(this::deliverPasskeyOnMainThread);
    }

    @JavascriptInterface
    public void onLoginFormDetected(@Nullable String ignoredPageUrl) {
        mMainHandler.post(this::deliverLoginFormOnMainThread);
    }

    void deliverPasskeyOnMainThread() {
        if (!mPasskeyDebounce.shouldShow(mClock.nowMs())) return;
        mHost.onPasskeyCeremonyDetected();
    }

    void deliverLoginFormOnMainThread() {
        if (!mLoginFormDebounce.shouldShow(mClock.nowMs())) return;
        mHost.onLoginFormDetected();
    }
}
