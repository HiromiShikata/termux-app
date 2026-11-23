package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;

import com.termux.R;

import org.junit.Test;

public class VoiceConsoleToolbarLabelTest {

    @Test
    public void labelOffersStopWhileConsoleRunsAndStartWhileStopped() {
        assertEquals(R.string.action_voice_console_start, VoiceConsoleToolbarLabel.stringResourceFor(false));
        assertEquals(R.string.action_voice_console_stop, VoiceConsoleToolbarLabel.stringResourceFor(true));
    }
}
