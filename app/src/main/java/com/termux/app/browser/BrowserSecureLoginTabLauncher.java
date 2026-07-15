package com.termux.app.browser;

import androidx.annotation.NonNull;

public final class BrowserSecureLoginTabLauncher {

    public interface SecureTabSink {

        void openInSecureTab(@NonNull BrowserSecureLoginTabLaunchMechanism mechanism, @NonNull String url);
    }

    public interface ExternalChromeSink {

        void openInExternalChrome(@NonNull String url);
    }

    public interface MechanismResolver {

        @NonNull
        BrowserSecureLoginTabLaunchMechanism resolveMechanism();
    }

    private final BrowserPasskeyOpenInChromeAction.TrustedCurrentUrlSource mTrustedUrlSource;

    private final MechanismResolver mMechanismResolver;

    private final SecureTabSink mSecureTabSink;

    private final ExternalChromeSink mExternalChromeSink;

    public BrowserSecureLoginTabLauncher(
            @NonNull BrowserPasskeyOpenInChromeAction.TrustedCurrentUrlSource trustedUrlSource,
            @NonNull MechanismResolver mechanismResolver,
            @NonNull SecureTabSink secureTabSink,
            @NonNull ExternalChromeSink externalChromeSink) {
        this.mTrustedUrlSource = trustedUrlSource;
        this.mMechanismResolver = mechanismResolver;
        this.mSecureTabSink = secureTabSink;
        this.mExternalChromeSink = externalChromeSink;
    }

    public void openTrustedCurrentUrlInSecureLoginTab() {
        String trustedUrl = mTrustedUrlSource.currentTrustedUrl();
        if (trustedUrl == null || trustedUrl.isEmpty()) return;
        BrowserSecureLoginTabLaunchMechanism mechanism = mMechanismResolver.resolveMechanism();
        if (mechanism == BrowserSecureLoginTabLaunchMechanism.EXTERNAL_CHROME) {
            mExternalChromeSink.openInExternalChrome(trustedUrl);
            return;
        }
        mSecureTabSink.openInSecureTab(mechanism, trustedUrl);
    }
}
