package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BackgroundPreparationDecisionTest {

    private static final class RetryCase {
        final boolean interrupted;
        final Optional<ApkUpdateBackgroundPreparation.Outcome> outcome;
        final boolean expectedRetry;

        RetryCase(boolean interrupted, Optional<ApkUpdateBackgroundPreparation.Outcome> outcome, boolean expectedRetry) {
            this.interrupted = interrupted;
            this.outcome = outcome;
            this.expectedRetry = expectedRetry;
        }
    }

    private static final List<RetryCase> RETRY_CASES = Arrays.asList(
        new RetryCase(true, Optional.empty(), true),
        new RetryCase(true, Optional.of(ApkUpdateBackgroundPreparation.Outcome.NOTIFIED), true),
        new RetryCase(false, Optional.empty(), false),
        new RetryCase(false, Optional.of(ApkUpdateBackgroundPreparation.Outcome.RELEASE_LIST_FAILED), true),
        new RetryCase(false, Optional.of(ApkUpdateBackgroundPreparation.Outcome.NOTIFIED), false),
        new RetryCase(false, Optional.of(ApkUpdateBackgroundPreparation.Outcome.NO_NEWER_RELEASE), false),
        new RetryCase(false, Optional.of(ApkUpdateBackgroundPreparation.Outcome.DOWNLOAD_FAILED), false)
    );

    @Test
    public void shouldRetryFollowsInterruptionAndOutcome() {
        for (int index = 0; index < RETRY_CASES.size(); index++) {
            RetryCase testCase = RETRY_CASES.get(index);
            boolean actual = BackgroundPreparationDecision.shouldRetry(testCase.outcome, testCase.interrupted);
            Assert.assertEquals(
                "case " + index + " interrupted=" + testCase.interrupted + " outcome=" + testCase.outcome,
                testCase.expectedRetry,
                actual);
        }
    }
}
