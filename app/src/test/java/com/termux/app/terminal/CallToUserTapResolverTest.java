package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CallToUserTapResolverTest {

    private static final class TapCase {
        final String requestedSessionName;
        final Set<String> liveSessionNames;
        final CallToUserTapResolver.Outcome expectedOutcome;

        TapCase(String requestedSessionName, Set<String> liveSessionNames,
                CallToUserTapResolver.Outcome expectedOutcome) {
            this.requestedSessionName = requestedSessionName;
            this.liveSessionNames = liveSessionNames;
            this.expectedOutcome = expectedOutcome;
        }
    }

    @Test
    public void tapResolvesToTheCallingSessionOnlyWhenItIsStillLive() {
        TapCase[] cases = {
            new TapCase(null, setOf("worker", "reviewer"), CallToUserTapResolver.Outcome.NOTHING_TO_SELECT),
            new TapCase("", setOf("worker"), CallToUserTapResolver.Outcome.NOTHING_TO_SELECT),
            new TapCase("worker", setOf("worker", "reviewer"), CallToUserTapResolver.Outcome.SELECT_CALLING_SESSION),
            new TapCase("reviewer", setOf("worker", "reviewer"), CallToUserTapResolver.Outcome.SELECT_CALLING_SESSION),
            new TapCase("worker", setOf("reviewer"), CallToUserTapResolver.Outcome.CALLING_SESSION_ENDED),
            new TapCase("worker", setOf(), CallToUserTapResolver.Outcome.CALLING_SESSION_ENDED),
        };
        for (TapCase testCase : cases) {
            Assert.assertEquals("requested=" + testCase.requestedSessionName + " live=" + testCase.liveSessionNames,
                testCase.expectedOutcome,
                CallToUserTapResolver.resolve(testCase.requestedSessionName, testCase.liveSessionNames));
        }
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }
}
