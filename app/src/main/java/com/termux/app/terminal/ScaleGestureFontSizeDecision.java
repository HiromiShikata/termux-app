package com.termux.app.terminal;

/**
 * Decides whether a pinch-to-zoom scale factor should change the terminal font size.
 *
 * Extracted as pure logic from {@link TermuxTerminalViewClient#onScale} so the threshold
 * behaviour can be unit tested without instantiating the Android view client. A scale within
 * the dead zone (0.9, 1.1) leaves the font size unchanged; outside it, a scale greater than 1
 * increases the font size and a scale less than 1 decreases it.
 */
public final class ScaleGestureFontSizeDecision {

    /** The decided font size action for a scale factor. */
    public enum Action {
        /** Scale is within the dead zone; the gesture is accumulated and the font size is unchanged. */
        NO_CHANGE,
        INCREASE_FONT_SIZE,
        DECREASE_FONT_SIZE
    }

    private ScaleGestureFontSizeDecision() {
    }

    /**
     * Decide the font size action for a pinch-to-zoom scale factor.
     *
     * @param scale The cumulative scale factor reported by the scale gesture detector.
     * @return The decided {@link Action}.
     */
    public static Action decide(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            return scale > 1.f ? Action.INCREASE_FONT_SIZE : Action.DECREASE_FONT_SIZE;
        }
        return Action.NO_CHANGE;
    }
}
