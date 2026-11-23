package com.termux.app.voiceconsole;

import com.termux.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class VoiceConsoleFailureMessages {

    private static final Set<String> CONNECTION_LOST_REASONS = new HashSet<>(Arrays.asList(
        "ice_failed", "event_channel_closed", "session_closed", "session_error"));

    private VoiceConsoleFailureMessages() {
    }

    public static int stringResourceFor(String reason) {
        return CONNECTION_LOST_REASONS.contains(reason)
            ? R.string.voice_console_connection_lost
            : R.string.voice_console_connection_failed;
    }
}
