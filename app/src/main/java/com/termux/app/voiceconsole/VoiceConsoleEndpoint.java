package com.termux.app.voiceconsole;

import androidx.annotation.Nullable;

public final class VoiceConsoleEndpoint {

    private final String mUrl;
    private final String mAccessToken;

    public VoiceConsoleEndpoint(String url, @Nullable String accessToken) {
        mUrl = url;
        mAccessToken = accessToken;
    }

    public String getUrl() {
        return mUrl;
    }

    @Nullable
    public String getAccessToken() {
        return mAccessToken;
    }
}
