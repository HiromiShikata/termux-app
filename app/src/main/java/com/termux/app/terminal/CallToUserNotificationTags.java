package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CallToUserNotificationTags {
    static final String TAG_PREFIX = "call-to-user:";

    private CallToUserNotificationTags() {
    }

    @NonNull
    public static String tagFor(@NonNull String sessionName) {
        return TAG_PREFIX + sessionName;
    }

    public static boolean isCallToUserTag(@Nullable String tag) {
        return tag != null && tag.startsWith(TAG_PREFIX);
    }

    @Nullable
    public static String sessionNameFromTag(@Nullable String tag) {
        if (!isCallToUserTag(tag)) return null;
        return tag.substring(TAG_PREFIX.length());
    }
}
