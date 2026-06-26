package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CallToUserTagControllerTest {

    private static final class RecordingTrigger implements CallToUserTagController.CallTrigger {
        final List<String> sessionKeys = new ArrayList<>();
        final List<String> reasons = new ArrayList<>();

        @Override
        public void onCallToUser(String sessionKey, String reason) {
            sessionKeys.add(sessionKey);
            reasons.add(reason);
        }
    }

    @Test
    public void triggersCallWithReasonOnNewTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "log <call-to-user>needs approval</call-to-user>");

        assertEquals(1, trigger.reasons.size());
        assertEquals("session-1", trigger.sessionKeys.get(0));
        assertEquals("needs approval", trigger.reasons.get(0));
    }

    @Test
    public void preservesJapaneseReason() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "<call-to-user>承認をお願いします</call-to-user>");

        assertEquals(1, trigger.reasons.size());
        assertEquals("承認をお願いします", trigger.reasons.get(0));
    }

    @Test
    public void doesNotTriggerWhenNoTagPresent() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "ordinary terminal output");

        assertTrue(trigger.reasons.isEmpty());
    }

    @Test
    public void doesNotRetriggerSameTagOnRedraw() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);
        String output = "log <call-to-user>same reason</call-to-user> log";

        controller.onSessionTextChanged("session-1", output);
        controller.onSessionTextChanged("session-1", output);

        assertEquals(1, trigger.reasons.size());
        assertEquals("same reason", trigger.reasons.get(0));
    }

    @Test
    public void triggersAgainForNewTagAfterPreviousTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "<call-to-user>first</call-to-user>");
        controller.onSessionTextChanged("session-1",
            "<call-to-user>first</call-to-user><call-to-user>second</call-to-user>");

        assertEquals(2, trigger.reasons.size());
        assertEquals("first", trigger.reasons.get(0));
        assertEquals("second", trigger.reasons.get(1));
    }

    @Test
    public void tracksReasonsPerSessionIndependently() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);
        String output = "<call-to-user>shared reason</call-to-user>";

        controller.onSessionTextChanged("session-1", output);
        controller.onSessionTextChanged("session-2", output);

        assertEquals(2, trigger.reasons.size());
        assertEquals("session-1", trigger.sessionKeys.get(0));
        assertEquals("session-2", trigger.sessionKeys.get(1));
    }

    @Test
    public void retriggersAfterSessionForgotten() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);
        String output = "<call-to-user>reason</call-to-user>";

        controller.onSessionTextChanged("session-1", output);
        controller.forgetSession("session-1");
        controller.onSessionTextChanged("session-1", output);

        assertEquals(2, trigger.reasons.size());
    }

    @Test
    public void ignoresNullSessionKey() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged(null, "<call-to-user>reason</call-to-user>");

        assertTrue(trigger.reasons.isEmpty());
    }

    @Test
    public void doesNotRecordANewCallWhenRescanningATranscriptThatStillContainsAnAlreadyFiredTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "<call-to-user>approval</call-to-user>");
        controller.onSessionTextChanged("session-1",
            "<call-to-user>approval</call-to-user>\nfollow-up output line 1");
        controller.onSessionTextChanged("session-1",
            "<call-to-user>approval</call-to-user>\nfollow-up output line 1\nfollow-up output line 2");

        assertEquals(1, trigger.reasons.size());
        assertEquals("approval", trigger.reasons.get(0));
    }

    @Test
    public void doesNotRecordANewCallWhenABottomSheetReFeedShowsAnEarlierCallWhileTheNewerCallScrolledOffTheTail() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "<call-to-user>first approval</call-to-user>\n");
        controller.onSessionTextChanged("session-1",
            "<call-to-user>first approval</call-to-user>\n<call-to-user>second approval</call-to-user>\n");

        assertEquals(2, trigger.reasons.size());

        // Opening the session-list bottom sheet re-feeds the scrollback window. The window shows the
        // earlier call at the front while the newer call is no longer rendered, which previously
        // collapsed the dedup boundary and re-recorded the earlier call with the current time.
        controller.onSessionTextChanged("session-1",
            "<call-to-user>first approval</call-to-user>\nlater plain output\n");

        assertEquals(2, trigger.reasons.size());
    }

    @Test
    public void recordsANewCallWhenAGenuinelyNewTagIsAppendedAfterAReFedWindow() {
        RecordingTrigger trigger = new RecordingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("session-1", "<call-to-user>first approval</call-to-user>\n");
        controller.onSessionTextChanged("session-1",
            "<call-to-user>first approval</call-to-user>\n<call-to-user>second approval</call-to-user>\n");
        controller.onSessionTextChanged("session-1",
            "<call-to-user>first approval</call-to-user>\nlater plain output\n");

        assertEquals(2, trigger.reasons.size());

        controller.onSessionTextChanged("session-1",
            "<call-to-user>first approval</call-to-user>\nlater plain output\n<call-to-user>third approval</call-to-user>\n");

        assertEquals(3, trigger.reasons.size());
        assertEquals("third approval", trigger.reasons.get(2));
    }
}
