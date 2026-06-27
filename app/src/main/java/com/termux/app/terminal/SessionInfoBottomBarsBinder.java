package com.termux.app.terminal;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import java.util.List;

public final class SessionInfoBottomBarsBinder {

    private SessionInfoBottomBarsBinder() {
    }

    public static void bind(@NonNull View root,
                            @Nullable SessionNewActivityStore store,
                            @Nullable String sessionName,
                            long nowMillis,
                            @NonNull Runnable scrollAction) {
        bindTimesLine(root, resolveSessionTimesLine(store, sessionName, nowMillis));
        bindCallToUserScene(root, resolvePendingCallToUserFooterDecision(store, sessionName),
            scrollAction);
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

    private static void bindCallToUserScene(@NonNull View root,
                                            @NonNull PendingCallToUserFooterDecision decision,
                                            @NonNull Runnable scrollAction) {
        View pendingCallToUserBar = root.findViewById(R.id.session_pending_call_to_user_bar);
        TextView pendingCallToUserText = root.findViewById(R.id.session_pending_call_to_user_text);
        ImageButton pendingCallToUserScrollButton =
            root.findViewById(R.id.session_pending_call_to_user_scroll_button);
        if (pendingCallToUserBar == null || pendingCallToUserText == null
            || pendingCallToUserScrollButton == null) {
            return;
        }
        PendingCallToUserFooterBinder.bind(pendingCallToUserBar, pendingCallToUserText,
            pendingCallToUserScrollButton, decision, scrollAction);
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
            store.getStatuslineReplyTimeMillis(sessionName),
            nowMillis);
    }

    @NonNull
    private static PendingCallToUserFooterDecision resolvePendingCallToUserFooterDecision(
            @Nullable SessionNewActivityStore store,
            @Nullable String sessionName) {
        if (store == null || sessionName == null) {
            return PendingCallToUserFooterDecision.resolve(SessionNewActivityTier.NONE, null);
        }
        return PendingCallToUserFooterDecision.resolve(
            store.tierFor(sessionName), mostRecentUnacknowledgedReason(store, sessionName));
    }

    @Nullable
    private static String mostRecentUnacknowledgedReason(@NonNull SessionNewActivityStore store,
                                                         @NonNull String sessionName) {
        List<String> reasons = store.getUnacknowledgedCallReasons(sessionName);
        if (reasons.isEmpty()) {
            return null;
        }
        return reasons.get(reasons.size() - 1);
    }
}
