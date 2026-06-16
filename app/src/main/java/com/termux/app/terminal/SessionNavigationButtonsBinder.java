package com.termux.app.terminal;

import android.view.View;

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
}
