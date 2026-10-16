package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Covers https://github.com/HiromiShikata/termux-app/issues/2040 : the bottom toolbar margin must
 * be recomputed only when the real keyboard's visibility signal actually changes value, never on
 * every layout pass triggered by a window-size-only change (such as a continuous split-screen
 * resize). {@link BottomToolbarMarginRecomputeGate} is the entire mechanism deciding whether
 * recomputation fires; it never looks at window size at all, so every one of the 6 parameterized
 * test cases in the issue collapses to: does {@code shouldRecomputeMargin} return {@code true} only
 * on the first call ever and on every call whose value differs from the immediately preceding call.
 */
public class BottomToolbarMarginRecomputeGateTest {

    /** One call into the gate and the result it must produce. */
    private static final class Call {
        private final boolean keyboardVisible;
        private final boolean expectedShouldRecompute;
        private final String description;

        Call(boolean keyboardVisible, boolean expectedShouldRecompute, String description) {
            this.keyboardVisible = keyboardVisible;
            this.expectedShouldRecompute = expectedShouldRecompute;
            this.description = description;
        }
    }

    /** A named, ordered sequence of calls made against one fresh gate instance. */
    private static final class Scenario {
        private final String name;
        private final Call[] calls;

        Scenario(String name, Call[] calls) {
            this.name = name;
            this.calls = calls;
        }
    }

    @Test
    public void shouldRecomputeMarginTracksOnlyTheImmediatelyPrecedingValue() {
        Scenario[] scenarios = new Scenario[]{
            new Scenario("startsWithKeyboardVisibleTrue", new Call[]{
                new Call(true, true, "first call ever (true) always recomputes"),
                new Call(true, false, "repeating true does not recompute"),
                new Call(true, false, "a second repeat of true still does not recompute"),
                new Call(false, true, "changing to false recomputes"),
                new Call(false, false, "repeating false does not recompute"),
                new Call(false, false, "a second repeat of false still does not recompute"),
                new Call(true, true, "changing back to true recomputes"),
                new Call(true, false, "repeating true after the change does not recompute"),
                new Call(true, false, "many repeats of true still do not recompute"),
                new Call(true, false, "yet another repeat of true still does not recompute"),
                new Call(false, true, "changing to false after many repeats of true still recomputes"),
            }),
            new Scenario("startsWithKeyboardVisibleFalse", new Call[]{
                new Call(false, true, "first call ever (false) always recomputes regardless of value"),
                new Call(false, false, "repeating false does not recompute"),
                new Call(true, true, "changing to true recomputes"),
            }),
        };

        for (Scenario scenario : scenarios) {
            BottomToolbarMarginRecomputeGate gate = new BottomToolbarMarginRecomputeGate();
            for (int i = 0; i < scenario.calls.length; i++) {
                Call call = scenario.calls[i];
                boolean actual = gate.shouldRecomputeMargin(call.keyboardVisible);
                Assert.assertEquals(
                    "scenario \"" + scenario.name + "\" call #" + i + " (" + call.description + ")",
                    call.expectedShouldRecompute, actual);
            }
        }
    }
}
