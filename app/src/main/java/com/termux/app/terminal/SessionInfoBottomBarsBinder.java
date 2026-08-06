package com.termux.app.terminal;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

public final class SessionInfoBottomBarsBinder {

    private SessionInfoBottomBarsBinder() {
    }

    public static void bind(@NonNull View root,
                            @Nullable SessionNewActivityStore store,
                            @Nullable String sessionName,
                            long nowMillis) {
        bindTimesLine(root, resolveSessionTimesLine(store, sessionName, nowMillis));
    }

    private static void bindTimesLine(@NonNull View root, @NonNull SessionTimesLine line) {
        TextView lastReplyBar = root.findViewById(R.id.session_last_reply_bar);
        if (lastReplyBar == null) return;
        if (line.isVisible()) {
            lastReplyBar.setText(line.getText());
            lastReplyBar.setVisibility(View.VISIBLE);
        } else {
            lastReplyBar.setText("");
            lastReplyBar.setVisibility(View.GONE);
        }
    }

    @NonNull
    private static SessionTimesLine resolveSessionTimesLine(@Nullable SessionNewActivityStore store,
                                                            @Nullable String sessionName,
                                                            long nowMillis) {
        if (store == null || sessionName == null) {
            return SessionTimesLine.hidden();
        }
        return SessionTimesLine.of(
            store.getStatuslineCallTimeMillis(sessionName),
            store.getStatuslineOutTimeMillis(sessionName),
            store.effectiveReplyTimeMillis(sessionName),
            store.getSubagentCount(sessionName),
            nowMillis);
    }
}
