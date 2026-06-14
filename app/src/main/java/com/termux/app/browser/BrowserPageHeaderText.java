package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserPageHeaderText {

    private static final String PROJECT_SESSION_SEPARATOR = " · ";

    private BrowserPageHeaderText() {
    }

    @NonNull
    public static BrowserPageHeader build(@Nullable String projectName, @Nullable String sessionName,
                                          @Nullable String title, @Nullable String url) {
        String contextLine = contextLine(projectName, sessionName);
        String titleLine = trimToEmpty(title);
        String compactUrlLine = BrowserGithubUrlShortener.shorten(url);
        if (titleLine.equals(compactUrlLine)) {
            titleLine = "";
        }
        return new BrowserPageHeader(contextLine, titleLine, compactUrlLine);
    }

    @NonNull
    private static String contextLine(@Nullable String projectName, @Nullable String sessionName) {
        String trimmedProject = trimToEmpty(projectName);
        String trimmedSession = trimToEmpty(sessionName);
        if (trimmedProject.isEmpty()) return trimmedSession;
        if (trimmedSession.isEmpty()) return trimmedProject;
        return trimmedProject + PROJECT_SESSION_SEPARATOR + trimmedSession;
    }

    @NonNull
    private static String trimToEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
