package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.browser.BrowserGithubUrlShortener;

public final class SessionNameBarContent {

    private final String name;

    private final String title;

    private SessionNameBarContent(@NonNull String name, @NonNull String title) {
        this.name = name;
        this.title = title;
    }

    @NonNull
    public static SessionNameBarContent of(@Nullable String sessionName, @Nullable String sessionTitle) {
        String prominentName = BrowserGithubUrlShortener.shorten(sessionName);
        String secondaryTitle = sessionTitle == null ? "" : sessionTitle.trim();
        return new SessionNameBarContent(prominentName, secondaryTitle);
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public boolean hasTitle() {
        return !title.isEmpty();
    }

    @NonNull
    public String getText() {
        if (!hasTitle()) return name;
        return name + "\n" + title;
    }

    public int getTitleStart() {
        if (!hasTitle()) return -1;
        return name.length() + 1;
    }

    public int getTitleEnd() {
        if (!hasTitle()) return -1;
        return getText().length();
    }
}
