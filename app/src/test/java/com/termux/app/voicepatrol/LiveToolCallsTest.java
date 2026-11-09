package com.termux.app.voicepatrol;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LiveToolCallsTest {
 @Test public void toolNamesAreConfigured(){assertEquals("session_reply_send",LiveToolCalls.SESSION_REPLY_SEND);assertEquals("session_skip",LiveToolCalls.SESSION_SKIP);assertEquals("owner_call_repeat",LiveToolCalls.OWNER_CALL_REPEAT);}
 @Test public void validReplyTextIsReturned(){LiveToolCalls.ReplyText reply=LiveToolCalls.replyTextOf("{\"replyText\":\"yes\"}");assertTrue(reply.isValid());assertEquals("yes",reply.getValue());}
 @Test public void missingReplyTextIsInvalid(){assertFalse(LiveToolCalls.replyTextOf("{\"text\":\"yes\"}").isValid());}
 @Test public void nonStringReplyTextIsInvalid(){assertFalse(LiveToolCalls.replyTextOf("{\"replyText\":3}").isValid());}
 @Test public void malformedArgumentsAreInvalid(){assertFalse(LiveToolCalls.replyTextOf("not json").isValid());assertFalse(LiveToolCalls.replyTextOf("[]").isValid());}
 @Test public void emptyReplyTextIsRepresentedForControllerValidation(){LiveToolCalls.ReplyText reply=LiveToolCalls.replyTextOf("{\"replyText\":\"\"}");assertTrue(reply.isValid());assertEquals("",reply.getValue());}
}
