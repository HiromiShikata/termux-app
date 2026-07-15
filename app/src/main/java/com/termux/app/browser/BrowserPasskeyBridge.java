package com.termux.app.browser;

import android.os.Handler;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPasskeyBridge {

    public interface Host {

        void onPasskeyCeremonyDetected(@NonNull String pageUrl);
    }

    private final Handler mMainHandler;

    private final Host mHost;

    private final BrowserPasskeyHintDebounce mDebounce;

    private final Clock mClock;

    interface Clock {

        long nowMs();
    }

    public BrowserPasskeyBridge(@NonNull Handler mainHandler, @NonNull Host host) {
        this(mainHandler, host, new BrowserPasskeyHintDebounce(), System::currentTimeMillis);
    }

    BrowserPasskeyBridge(
            @NonNull Handler mainHandler,
            @NonNull Host host,
            @NonNull BrowserPasskeyHintDebounce debounce,
            @NonNull Clock clock) {
        this.mMainHandler = mainHandler;
        this.mHost = host;
        this.mDebounce = debounce;
        this.mClock = clock;
    }

    @JavascriptInterface
    public void onPasskeyCeremonyDetected(@Nullable String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) return;
        mMainHandler.post(() -> deliverOnMainThread(pageUrl));
    }

    void deliverOnMainThread(@NonNull String pageUrl) {
        if (!mDebounce.shouldShow(mClock.nowMs())) return;
        mHost.onPasskeyCeremonyDetected(pageUrl);
    }
}
