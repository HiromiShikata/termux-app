package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VoiceConsoleEventTest {

    @Test
    public void onlySessionClosedAndErrorEventsEndTheSession() {
        String[][] eventJsonAndExpectedReason = {
            {"{\"type\":\"session.closed\"}", "session_closed"},
            {"{\"type\":\"error\",\"error\":{\"message\":\"boom\"}}", "session_error"},
            {"{\"type\":\"response.done\"}", null},
            {"{\"type\":\"session.created\"}", null},
            {"{}", null},
            {"not json", null},
        };
        for (String[] testCase : eventJsonAndExpectedReason) {
            assertEquals("event " + testCase[0], testCase[1], VoiceConsoleEvent.endingReasonOf(testCase[0]));
        }
    }

    @Test
    public void sessionCloseEventIsTheEventTheAppSendsOnStop() throws Exception {
        assertEquals("session.close", new org.json.JSONObject(VoiceConsoleEvent.SESSION_CLOSE_EVENT_JSON)
            .optString("type"));
    }
}
