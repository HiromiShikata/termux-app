package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NotificationRepostDecision<T> {

    @Nullable
    private T mPosted;

    public boolean isNeededFor(@NonNull T content) {
        if (content.equals(mPosted)) {
            return false;
        }
        mPosted = content;
        return true;
    }
}
