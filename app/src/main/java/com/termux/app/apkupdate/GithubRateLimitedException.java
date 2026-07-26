package com.termux.app.apkupdate;

import java.io.IOException;

public final class GithubRateLimitedException extends IOException {

    public GithubRateLimitedException(String message) {
        super(message);
    }
}
