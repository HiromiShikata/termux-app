package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LiveToolCallsTest {

    @Test
    public void toolNamesAreExactlyTheConfiguredFunctionNames() {
        assertEquals("session_reply_send", LiveToolCalls.SESSION_REPLY_SEND);
        assertEquals("session_skip", LiveToolCalls.SESSION_SKIP);
        assertEquals("owner_call_repeat", LiveToolCalls.OWNER_CALL_REPEAT);
    }

    @Test
    public void replyTextIsReadOnlyFromTheReplyTextProperty() {
        assertEquals("yes", LiveToolCalls.replyTextOf("{\"replyText\":\"yes\"}").getValue());
        assertFalse(LiveToolCalls.replyTextOf("{\"text\":\"yes\"}").isValid());
        assertFalse(LiveToolCalls.replyTextOf("[]").isValid());
        assertFalse(LiveToolCalls.replyTextOf("{\"replyText\":3}").isValid());
    }

    @Test
    public void emptyReplyTextIsAcceptedAsAValueSoTheCallerDecidesWhetherToSend() {
        assertTrue(LiveToolCalls.replyTextOf("{\"replyText\":\"\"}").isValid());
        assertEquals("", LiveToolCalls.replyTextOf("{\"replyText\":\"\"}").getValue());
    }
}
