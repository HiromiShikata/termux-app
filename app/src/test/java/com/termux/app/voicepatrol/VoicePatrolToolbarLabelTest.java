package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import com.termux.R;

import org.junit.Test;

public class VoicePatrolToolbarLabelTest {

    @Test
    public void labelOffersStopWhilePatrolRunsAndStartWhileStopped() {
        assertEquals(R.string.action_voice_patrol_start, VoicePatrolToolbarLabel.stringResourceFor(false));
        assertEquals(R.string.action_voice_patrol_stop, VoicePatrolToolbarLabel.stringResourceFor(true));
    }
}
