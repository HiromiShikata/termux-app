package com.termux.app.apkupdate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UpdateTagUpdateControllerTest {

    private static final class RecordingTrigger implements UpdateTagUpdateController.ReasonTrigger {
        final List<String> reasons = new ArrayList<>();

        @Override
        public void onUpdateRequested(String reason) {
            reasons.add(reason);
        }
    }

    @Test
    public void triggersUpdateWithReasonOnNewTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);

        controller.onSessionTextChanged("session-1", "log <update-termux-up>new build</update-termux-up>");

        assertEquals(1, trigger.reasons.size());
        assertEquals("new build", trigger.reasons.get(0));
    }

    @Test
    public void doesNotTriggerWhenNoTagPresent() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);

        controller.onSessionTextChanged("session-1", "ordinary terminal output");

        assertTrue(trigger.reasons.isEmpty());
    }

    @Test
    public void doesNotRetriggerSameTagOnRedraw() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);
        String output = "log <update-termux-up>same reason</update-termux-up> log";

        controller.onSessionTextChanged("session-1", output);
        controller.onSessionTextChanged("session-1", output);

        assertEquals(1, trigger.reasons.size());
        assertEquals("same reason", trigger.reasons.get(0));
    }

    @Test
    public void triggersAgainForNewTagAfterPreviousTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);

        controller.onSessionTextChanged("session-1", "<update-termux-up>first</update-termux-up>");
        controller.onSessionTextChanged("session-1",
            "<update-termux-up>first</update-termux-up><update-termux-up>second</update-termux-up>");

        assertEquals(2, trigger.reasons.size());
        assertEquals("first", trigger.reasons.get(0));
        assertEquals("second", trigger.reasons.get(1));
    }

    @Test
    public void tracksReasonsPerSessionIndependently() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);
        String output = "<update-termux-up>shared reason</update-termux-up>";

        controller.onSessionTextChanged("session-1", output);
        controller.onSessionTextChanged("session-2", output);

        assertEquals(2, trigger.reasons.size());
    }

    @Test
    public void retriggersAfterSessionForgotten() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);
        String output = "<update-termux-up>reason</update-termux-up>";

        controller.onSessionTextChanged("session-1", output);
        controller.forgetSession("session-1");
        controller.onSessionTextChanged("session-1", output);

        assertEquals(2, trigger.reasons.size());
    }

    @Test
    public void ignoresNullSessionKey() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);

        controller.onSessionTextChanged(null, "<update-termux-up>reason</update-termux-up>");

        assertTrue(trigger.reasons.isEmpty());
    }
}
