package com.termux.app.terminal;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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

    public static void applyDirectionCountBadges(@NonNull TextView previousSessionCountBadge,
                                                 @NonNull TextView nextSessionCountBadge,
                                                 @NonNull SessionActivityDirection direction) {
        applyCountBadge(previousSessionCountBadge, direction.getActiveAboveCount());
        applyCountBadge(nextSessionCountBadge, direction.getActiveBelowCount());
    }

    public static void applyCallingSessionCountBadge(@NonNull TextView countBadge, int count) {
        applyCountBadge(countBadge, count);
    }

    static void applyCountBadge(@NonNull TextView countBadge, int count) {
        if (count <= 0) {
            countBadge.setText("");
            countBadge.setVisibility(View.GONE);
            return;
        }
        countBadge.setText(String.valueOf(count));
        countBadge.setVisibility(View.VISIBLE);
    }
}
