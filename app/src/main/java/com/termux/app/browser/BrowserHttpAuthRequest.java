package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserHttpAuthRequest {

    private final String mUsername;

    private final String mPassword;

    public BrowserHttpAuthRequest(@Nullable String username, @Nullable String password) {
        this.mUsername = username == null ? "" : username;
        this.mPassword = password == null ? "" : password;
    }

    @NonNull
    public String getUsername() {
        return mUsername;
    }

    @NonNull
    public String getPassword() {
        return mPassword;
    }
}
