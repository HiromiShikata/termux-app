package com.termux.app.terminal;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public final class SessionNavigationButtonsBinder {

    public interface SessionDirectionListener {
        void onSessionDirection(boolean forward);
    }

    private SessionNavigationButtonsBinder() {
    }

    public static void bind(View previousSessionButton, View nextSessionButton,
                            SessionDirectionListener listener) {
        previousSessionButton.setOnClickListener(v -> listener.onSessionDirection(false));
        nextSessionButton.setOnClickListener(v -> listener.onSessionDirection(true));
    }

    public static void applyDirectionGlow(@NonNull ImageView previousSessionButton,
                                          @NonNull ImageView nextSessionButton,
                                          @NonNull SessionBellDirection direction,
                                          int glowColor, int defaultColor) {
        previousSessionButton.setColorFilter(direction.hasBellAbove() ? glowColor : defaultColor);
        nextSessionButton.setColorFilter(direction.hasBellBelow() ? glowColor : defaultColor);
    }
}
