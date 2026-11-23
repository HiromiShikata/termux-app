package com.termux.app.voiceconsole;

import com.termux.R;

public final class VoiceConsoleToolbarLabel {

    private VoiceConsoleToolbarLabel() {
    }

    public static int stringResourceFor(boolean consoleRunning) {
        return consoleRunning ? R.string.action_voice_console_stop : R.string.action_voice_console_start;
    }
}
