package com.termux.app.terminal;

import androidx.annotation.NonNull;

/**
 * One owner call detected in a session transcript: the opaque trigger value carried by the literal
 * {@code <call-to-user>} tag that fired it, and the human-readable reason to display for it.
 *
 * <p>The trigger value is never display content. It identifies the call for deduplication only, so
 * the app behaves identically whether the emitter puts an approval timestamp or arbitrary message
 * text inside the literal tag. The display reason comes from the candidate {@code
 * <call-to-user-pending>} tag the agent printed, and falls back to the trigger value only when the
 * session has never shown a candidate, so a call is recorded rather than silently dropped.
 */
public final class ApprovedCallToUser {

    @NonNull
    private final String mTriggerValue;

    @NonNull
    private final String mDisplayReason;

    public ApprovedCallToUser(@NonNull String triggerValue, @NonNull String displayReason) {
        mTriggerValue = triggerValue;
        mDisplayReason = displayReason;
    }

    @NonNull
    public String getTriggerValue() {
        return mTriggerValue;
    }

    @NonNull
    public String getDisplayReason() {
        return mDisplayReason;
    }
}
