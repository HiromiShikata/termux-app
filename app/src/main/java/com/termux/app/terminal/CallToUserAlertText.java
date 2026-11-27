package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CallToUserAlertText {

    private CallToUserAlertText() {
    }

    @NonNull
    public static String forFirstPost(@Nullable String body, @NonNull String callText) {
        return body == null ? callText : body;
    }
}
