package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CallToUserAlertCancellationTest {

    private static final class CancelCase {
        final Set<String> knownAlertSessionNames;
        final Set<String> pendingSessionNames;
        final Set<String> expectedSessionNamesToCancel;

        CancelCase(Set<String> knownAlertSessionNames, Set<String> pendingSessionNames,
                   Set<String> expectedSessionNamesToCancel) {
            this.knownAlertSessionNames = knownAlertSessionNames;
            this.pendingSessionNames = pendingSessionNames;
            this.expectedSessionNamesToCancel = expectedSessionNamesToCancel;
        }
    }

    @Test
    public void onlyAlertsWhoseSessionIsNoLongerPendingAreCancelled() {
        CancelCase[] cases = {
            new CancelCase(setOf(), setOf(), setOf()),
            new CancelCase(setOf("worker"), setOf("worker"), setOf()),
            new CancelCase(setOf("worker"), setOf(), setOf("worker")),
            new CancelCase(setOf("worker", "reviewer"), setOf("reviewer"), setOf("worker")),
            new CancelCase(setOf("worker"), setOf("reviewer"), setOf("worker")),
            new CancelCase(setOf(), setOf("reviewer"), setOf()),
        };
        for (CancelCase testCase : cases) {
            Assert.assertEquals(
                "known=" + testCase.knownAlertSessionNames + " pending=" + testCase.pendingSessionNames,
                testCase.expectedSessionNamesToCancel,
                CallToUserAlertCancellation.sessionsToCancel(testCase.knownAlertSessionNames,
                    testCase.pendingSessionNames));
        }
    }

    @Test
    public void inputSetsAreNotModified() {
        Set<String> known = setOf("worker", "reviewer");
        Set<String> pending = setOf("reviewer");

        CallToUserAlertCancellation.sessionsToCancel(known, pending);

        Assert.assertEquals(setOf("worker", "reviewer"), known);
        Assert.assertEquals(setOf("reviewer"), pending);
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }
}
