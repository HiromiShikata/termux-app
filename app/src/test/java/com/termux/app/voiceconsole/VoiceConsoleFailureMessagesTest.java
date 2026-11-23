package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;

import com.termux.R;

import org.junit.Test;

public class VoiceConsoleFailureMessagesTest {

    @Test
    public void reasonsMapToConnectionLostOrConnectionFailedMessages() {
        String[][] reasonAndExpectedMessage = {
            {"ice_failed", "lost"},
            {"event_channel_closed", "lost"},
            {"session_closed", "lost"},
            {"session_error", "lost"},
            {"network_error", "failed"},
            {"HTTP 500", "failed"},
            {"ice_gathering_timeout", "failed"},
            {"remote_description_failed", "failed"},
        };
        for (String[] testCase : reasonAndExpectedMessage) {
            int expected = "lost".equals(testCase[1])
                ? R.string.voice_console_connection_lost
                : R.string.voice_console_connection_failed;
            assertEquals("reason " + testCase[0], expected, VoiceConsoleFailureMessages.stringResourceFor(testCase[0]));
        }
    }
}
