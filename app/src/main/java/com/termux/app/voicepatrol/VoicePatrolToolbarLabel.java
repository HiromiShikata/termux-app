package com.termux.app.voicepatrol;

import com.termux.R;

public final class VoicePatrolToolbarLabel {

    private VoicePatrolToolbarLabel() {
    }

    public static int stringResourceFor(boolean patrolRunning) {
        return patrolRunning ? R.string.action_voice_patrol_stop : R.string.action_voice_patrol_start;
    }
}
