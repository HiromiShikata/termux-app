package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CallToUserRescanAfterScrollbackTrimTest {

    private static final String FIRST_STATUSLINE = "call:10:00:01 out:10:00:02 reply:10:00:00\n";
    private static final String SECOND_STATUSLINE = "call:10:01:01 out:10:01:02 reply:10:00:00\n";
    private static final String THIRD_STATUSLINE = "call:10:02:01 out:10:02:02 reply:10:00:00\n";
    private static final String FIRST_CALL = "<call-to-user>first call</call-to-user>\n";
    private static final String SECOND_CALL = "<call-to-user>second call</call-to-user>\n";
    private static final String THIRD_CALL = "<call-to-user>third call</call-to-user>\n";

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
        private final CallToUserAlertLedger mLedger = new CallToUserAlertLedger(new InMemoryStorage());
        final List<String> mAlerts = new ArrayList<>();

        @Override
        public void onCallToUser(String sessionKey, String reason, String callCycleKey) {
            Assert.fail("the production trigger must reach the alert gate through onCallToUserInScan");
        }

        @Override
        public void onCallToUserInScan(String sessionKey, String reason, String callCycleKey,
                                       boolean newestInScan) {
            if (newestInScan && mLedger.markAlertedIfNew(sessionKey, callCycleKey, reason)) {
                mAlerts.add(reason);
            }
        }
    }

    @Test
    public void aCallRediscoveredAfterScrollbackTrimsItsStatuslineNeverAlertsAgain() {
        AlertingTrigger trigger = new AlertingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("worker", FIRST_STATUSLINE + FIRST_CALL + SECOND_STATUSLINE + SECOND_CALL);
        int alertsAfterFirstScan = trigger.mAlerts.size();

        controller.onSessionTextChanged("worker", FIRST_CALL + SECOND_STATUSLINE + SECOND_CALL);

        Assert.assertEquals("a call re-keyed by scrollback trimming must not alert again: alerts="
            + trigger.mAlerts, alertsAfterFirstScan, trigger.mAlerts.size());
    }

    @Test
    public void aNewestCallRekeyedWithoutItsStatuslineNeverAlertsAgain() {
        AlertingTrigger trigger = new AlertingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("worker", FIRST_STATUSLINE + FIRST_CALL);
        int alertsAfterFirstScan = trigger.mAlerts.size();

        controller.onSessionTextChanged("worker", FIRST_CALL);

        Assert.assertEquals("the newest call re-keyed without its statusline must not alert again: alerts="
            + trigger.mAlerts, alertsAfterFirstScan, trigger.mAlerts.size());
    }

    @Test
    public void onlyTheNewestCallOfTheFirstScanAlerts() {
        AlertingTrigger trigger = new AlertingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);

        controller.onSessionTextChanged("worker", FIRST_STATUSLINE + FIRST_CALL + SECOND_STATUSLINE + SECOND_CALL);

        Assert.assertEquals(Arrays.asList("second call"), trigger.mAlerts);
    }

    @Test
    public void aGenuinelyNewCallAppendedAfterTrimmingStillAlerts() {
        AlertingTrigger trigger = new AlertingTrigger();
        CallToUserTagController controller = new CallToUserTagController(trigger);
        controller.onSessionTextChanged("worker", FIRST_STATUSLINE + FIRST_CALL + SECOND_STATUSLINE + SECOND_CALL);
        controller.onSessionTextChanged("worker", FIRST_CALL + SECOND_STATUSLINE + SECOND_CALL);

        controller.onSessionTextChanged("worker",
            FIRST_CALL + SECOND_STATUSLINE + SECOND_CALL + THIRD_STATUSLINE + THIRD_CALL);

        Assert.assertEquals(Arrays.asList("second call", "third call"), trigger.mAlerts);
    }
}
