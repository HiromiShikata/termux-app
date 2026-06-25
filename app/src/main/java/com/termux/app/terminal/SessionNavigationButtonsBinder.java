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

    public static void applyDirectionTier(@NonNull ImageView previousSessionButton,
                                          @NonNull ImageView nextSessionButton,
                                          @NonNull SessionActivityDirection direction,
                                          int redColor, int defaultColor) {
        int tierColor = tierColor(direction.getTier(), redColor, defaultColor);
        previousSessionButton.setColorFilter(direction.hasActiveAbove() ? tierColor : defaultColor);
        nextSessionButton.setColorFilter(direction.hasActiveBelow() ? tierColor : defaultColor);
    }

    static int tierColor(@NonNull SessionNewActivityTier tier, int redColor, int defaultColor) {
        return tier == SessionNewActivityTier.RED ? redColor : defaultColor;
    }
}
