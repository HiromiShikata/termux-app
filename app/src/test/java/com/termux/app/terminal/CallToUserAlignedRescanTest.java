package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CallToUserAlignedRescanTest {

    private static final String FIRST_STATUSLINE = "call:10:00:01 out:10:00:02 reply:10:00:00\n";
    private static final String FIRST_CALL = "<call-to-user>first call</call-to-user>\n";
    private static final String OUTPUT_LINE = "some output\n";
    private static final String SECOND_CALL = "<call-to-user>second call</call-to-user>\n";

    private static final class InMemoryStorage implements CallToUserAlertLedger.Storage {
        private String mValue;

        @Override
        public String read() {
            return mValue;
        }

        @Override
        public void write(String value) {
            mValue = value;
        }
    }

    private static final class AlertingTrigger implements CallToUserTagController.CallTrigger {
        CallToUserAlertLedger mLedger = new CallToUserAlertLedger(new InMemoryStorage());
        final List<String> mAlerts = new ArrayList<>();

        @Override
        public void onCallToUser(String sessionKey, String reason, String callCycleKey) {
            Assert.fail("the production trigger must reach the alert gate through onCallToUserInScan");
        }

        @Override
        public void onCallToUserInScan(String sessionKey, String reason, String callCycleKey,
                                       boolean mayAlert) {
            if (CallToUserAlertGate.shouldAlert(mLedger, mayAlert, true, sessionKey, callCycleKey, reason)) {
                mAlerts.add(reason);
            }
        }

        @Override
        public boolean hasAlertedCall(String sessionKey, String callCycleKey, String tagValue) {
            return mLedger.isAlreadyAlerted(sessionKey, callCycleKey, tagValue);
        }
    }

    @Test
    public void aNewlyAppendedCallWithTheSameTextAsAnAlertedCallAlertsWhenItsStatuslineIsGone() {
        AlertingTrigger trigger = new AlertingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);
        controller.onSessionTextChanged("worker", FIRST_STATUSLINE + FIRST_CALL);

        controller.onSessionTextChanged("worker", FIRST_CALL + OUTPUT_LINE + FIRST_CALL);

        Assert.assertEquals(Arrays.asList("first call", "first call"), trigger.mAlerts);
    }

    @Test
    public void aTrimmedCallDoesNotAlertWhileANewlyAppendedCallAlerts() {
        AlertingTrigger trigger = new AlertingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);
        controller.onSessionTextChanged("worker", FIRST_STATUSLINE + FIRST_CALL);

        controller.onSessionTextChanged("worker", FIRST_CALL + OUTPUT_LINE + FIRST_CALL);

        Assert.assertEquals("only the newly appended call may alert: alerts=" + trigger.mAlerts,
            2, trigger.mAlerts.size());
    }

    @Test
    public void aFirstScanAfterARestartTreatsAlreadyAlertedBareCallsAsSeenAndAlertsTheAppendedOne() {
        InMemoryStorage storage = new InMemoryStorage();
        new CallToUserAlertLedger(storage).recordAlerted("worker", "first call", "first call");
        AlertingTrigger restartedTrigger = new AlertingTrigger();
        restartedTrigger.mLedger = new CallToUserAlertLedger(storage);
        CallToUserTagController restartedController = new CallToUserTagController(restartedTrigger);

        restartedController.onSessionTextChanged("worker", FIRST_CALL + OUTPUT_LINE + SECOND_CALL);

        Assert.assertEquals(Arrays.asList("second call"), restartedTrigger.mAlerts);
    }
}
