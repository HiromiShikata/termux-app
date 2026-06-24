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

        controller.onSessionTextChanged("session-1", "log <update-termux-app>new build</update-termux-app>");

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
        String output = "log <update-termux-app>same reason</update-termux-app> log";

        controller.onSessionTextChanged("session-1", output);
        controller.onSessionTextChanged("session-1", output);

        assertEquals(1, trigger.reasons.size());
        assertEquals("same reason", trigger.reasons.get(0));
    }

    @Test
    public void triggersAgainForNewTagAfterPreviousTag() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);

        controller.onSessionTextChanged("session-1", "<update-termux-app>first</update-termux-app>");
        controller.onSessionTextChanged("session-1",
            "<update-termux-app>first</update-termux-app><update-termux-app>second</update-termux-app>");

        assertEquals(2, trigger.reasons.size());
        assertEquals("first", trigger.reasons.get(0));
        assertEquals("second", trigger.reasons.get(1));
    }

    @Test
    public void tracksReasonsPerSessionIndependently() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);
        String output = "<update-termux-app>shared reason</update-termux-app>";

        controller.onSessionTextChanged("session-1", output);
        controller.onSessionTextChanged("session-2", output);

        assertEquals(2, trigger.reasons.size());
    }

    @Test
    public void retriggersAfterSessionForgotten() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);
        String output = "<update-termux-app>reason</update-termux-app>";

        controller.onSessionTextChanged("session-1", output);
        controller.forgetSession("session-1");
        controller.onSessionTextChanged("session-1", output);

        assertEquals(2, trigger.reasons.size());
    }

    @Test
    public void ignoresNullSessionKey() {
        RecordingTrigger trigger = new RecordingTrigger();
        UpdateTagUpdateController controller = new UpdateTagUpdateController(trigger);

        controller.onSessionTextChanged(null, "<update-termux-app>reason</update-termux-app>");

        assertTrue(trigger.reasons.isEmpty());
    }
}
