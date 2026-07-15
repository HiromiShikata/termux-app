package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPasskeyOpenInChromeAction {

    public interface TrustedCurrentUrlSource {

        @Nullable
        String currentTrustedUrl();
    }

    public interface OpenInChromeSink {

        void openInChrome(@NonNull String url);
    }

    private final TrustedCurrentUrlSource mTrustedUrlSource;

    private final OpenInChromeSink mOpenSink;

    public BrowserPasskeyOpenInChromeAction(
            @NonNull TrustedCurrentUrlSource trustedUrlSource,
            @NonNull OpenInChromeSink openSink) {
        this.mTrustedUrlSource = trustedUrlSource;
        this.mOpenSink = openSink;
    }

    public void openTrustedCurrentUrlInChrome() {
        String trustedUrl = mTrustedUrlSource.currentTrustedUrl();
        if (trustedUrl == null || trustedUrl.isEmpty()) return;
        mOpenSink.openInChrome(trustedUrl);
    }
}
