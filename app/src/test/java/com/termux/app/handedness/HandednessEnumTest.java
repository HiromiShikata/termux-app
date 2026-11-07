package com.termux.app.handedness;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HandednessEnumTest {

    @Test
    public void fromLeftHandedModeEnabledReturnsExpectedHandedness() {
        Case[] cases = {
            new Case(true, Handedness.LEFT_HANDED),
            new Case(false, Handedness.RIGHT_HANDED)
        };

        for (Case testCase : cases) {
            Assert.assertEquals(testCase.expected,
                Handedness.fromLeftHandedModeEnabled(testCase.enabled));
        }
    }

    private static class Case {
        private final boolean enabled;
        private final Handedness expected;

        private Case(boolean enabled, Handedness expected) {
            this.enabled = enabled;
            this.expected = expected;
        }
    }
}
