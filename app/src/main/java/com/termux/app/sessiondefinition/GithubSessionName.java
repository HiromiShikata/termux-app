package com.termux.app.sessiondefinition;

import androidx.annotation.Nullable;

public final class GithubSessionName {

    public static final String GITHUB_URL_PREFIX = "https://github.com/";

    private GithubSessionName() {
    }

    public static boolean isGithubSessionName(@Nullable String sessionName) {
        return sessionName != null && sessionName.startsWith(GITHUB_URL_PREFIX);
    }
}
