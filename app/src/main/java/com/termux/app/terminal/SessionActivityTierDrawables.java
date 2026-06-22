package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.R;

public final class SessionActivityTierDrawables {

    private SessionActivityTierDrawables() {
    }

    public static int dotDrawableRes(@NonNull SessionNewActivityTier tier) {
        switch (tier) {
            case RED:
                return R.drawable.ic_session_activity_dot_red;
            case YELLOW:
                return R.drawable.ic_session_activity_dot_yellow;
            case NONE:
            default:
                return R.drawable.ic_session_activity_dot_placeholder;
        }
    }
}
