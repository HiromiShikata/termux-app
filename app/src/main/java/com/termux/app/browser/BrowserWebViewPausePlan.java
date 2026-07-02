package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BrowserWebViewPausePlan<T> {

    private final List<T> mToPause;

    private final List<T> mToResume;

    private final boolean mTimersActive;

    private BrowserWebViewPausePlan(@NonNull List<T> toPause, @NonNull List<T> toResume, boolean timersActive) {
        mToPause = Collections.unmodifiableList(new ArrayList<>(toPause));
        mToResume = Collections.unmodifiableList(new ArrayList<>(toResume));
        mTimersActive = timersActive;
    }

    @NonNull
    public static <T> BrowserWebViewPausePlan<T> resolve(
        @NonNull List<T> allWebViews,
        @Nullable T displayedWebView,
        boolean browserVisible,
        boolean appForegrounded) {
        boolean displayedShouldBeActive =
            browserVisible && appForegrounded && displayedWebView != null;
        List<T> toPause = new ArrayList<>();
        List<T> toResume = new ArrayList<>();
        for (T webView : allWebViews) {
            if (webView == null) continue;
            boolean isDisplayed = webView == displayedWebView;
            if (isDisplayed && displayedShouldBeActive) {
                toResume.add(webView);
            } else {
                toPause.add(webView);
            }
        }
        return new BrowserWebViewPausePlan<>(toPause, toResume, displayedShouldBeActive);
    }

    @NonNull
    public List<T> getWebViewsToPause() {
        return mToPause;
    }

    @NonNull
    public List<T> getWebViewsToResume() {
        return mToResume;
    }

    public boolean shouldTimersBeActive() {
        return mTimersActive;
    }
}
