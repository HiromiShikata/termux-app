package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionRowContent {

    @NonNull
    private final String boundText;

    SessionRowContent(@NonNull String boundText) {
        this.boundText = boundText;
    }

    @NonNull
    public String getBoundText() {
        return boundText;
    }

    static boolean sameContent(@Nullable SessionRowContent firstContent,
                               @Nullable SessionRowContent secondContent) {
        if (firstContent == null || secondContent == null) {
            return firstContent == secondContent;
        }
        return firstContent.boundText.equals(secondContent.boundText);
    }
}
