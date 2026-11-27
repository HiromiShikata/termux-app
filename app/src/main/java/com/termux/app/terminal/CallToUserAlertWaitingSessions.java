package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The sessions whose call-to-user alert is still waiting for the owner's answer.
 *
 * <p>A call the alert was posted for is answered by a genuine reply at or after the moment of the call,
 * which is the same rule the owner call window uses to hide an answered call. Keystrokes do not answer
 * it. A session with no recorded call time has no such rule, so it waits while the store reports it
 * pending.
 */
public final class CallToUserAlertWaitingSessions {

    private CallToUserAlertWaitingSessions() {
    }

    @NonNull
    public static Set<String> of(@NonNull Map<String, Long> callAlertedAtMillisBySession,
                                 @NonNull Set<String> storePendingSessionNames,
                                 @NonNull Map<String, Long> genuineReplyMillisBySession) {
        Set<String> waiting = new HashSet<>();
        for (String sessionName : storePendingSessionNames) {
            if (!callAlertedAtMillisBySession.containsKey(sessionName)) {
                waiting.add(sessionName);
            }
        }
        for (Map.Entry<String, Long> alerted : callAlertedAtMillisBySession.entrySet()) {
            Long genuineReplyMillis = genuineReplyMillisBySession.get(alerted.getKey());
            if (genuineReplyMillis == null || genuineReplyMillis < alerted.getValue()) {
                waiting.add(alerted.getKey());
            }
        }
        return waiting;
    }
}
