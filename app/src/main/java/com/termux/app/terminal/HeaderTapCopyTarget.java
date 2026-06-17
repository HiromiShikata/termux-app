package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class HeaderTapCopyTarget {

    public enum Kind {
        PROJECT_BROWSER_URL,
        SESSION_NAME
    }

    private final Kind kind;

    private final String text;

    private HeaderTapCopyTarget(@NonNull Kind kind, @Nullable String text) {
        this.kind = kind;
        this.text = text;
    }

    @NonNull
    public static HeaderTapCopyTarget resolve(boolean projectBrowserVisible,
                                              @Nullable String projectBrowserUrl,
                                              @Nullable String sessionNameForCopy) {
        if (projectBrowserVisible && projectBrowserUrl != null && !projectBrowserUrl.isEmpty()) {
            return new HeaderTapCopyTarget(Kind.PROJECT_BROWSER_URL, projectBrowserUrl);
        }
        return new HeaderTapCopyTarget(Kind.SESSION_NAME, sessionNameForCopy);
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public boolean isProjectBrowserUrl() {
        return kind == Kind.PROJECT_BROWSER_URL;
    }

    public boolean isEmpty() {
        return text == null || text.isEmpty();
    }
}
