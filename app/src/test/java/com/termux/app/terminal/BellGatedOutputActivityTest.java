package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Locks in that a bell only advances out: (last output activity) when genuinely new process output
 * accompanies it. Both terminal session clients now route their onBell handling through the same
 * {@link SessionOutputProgressTracker} genuine-output gate that onTextChanged uses, instead of
 * recording output activity unconditionally on every BEL byte. This models that gated recording: a
 * recordOutputActivity is performed only when the tracker reports new genuine output for the bell's
 * genuine-output version.
 */
public class BellGatedOutputActivityTest {

    private static final String SESSION = "worker";

    /** Mirrors the client gate: record output activity for the bell only when genuine output advanced. */
    private static void recordBellOutputActivityIfGenuine(SessionOutputProgressTracker tracker,
                                                          SessionNewActivityStore store,
                                                          long genuineOutputVersion, long nowMillis) {
        if (tracker.hasNewOutput(SESSION, genuineOutputVersion)) {
            store.recordOutputActivity(SESSION, nowMillis);
        }
    }

    @Test
    public void repeatedBellsWithoutNewGenuineOutputDoNotAdvanceOutTime() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        SessionNewActivityStore store = new SessionNewActivityStore();

        // Seed the baseline at the current genuine-output version (the first observation seeds and
        // reports no new output), then record one genuine output so out: has a value.
        recordBellOutputActivityIfGenuine(tracker, store, 100L, 1_000L);
        recordBellOutputActivityIfGenuine(tracker, store, 101L, 2_000L);
        Assert.assertEquals(Long.valueOf(2_000L), store.getLastOutputActivityTimeMillis(SESSION));

        // A burst of bells arrives while the genuine-output version stays at 101 (e.g. bells echoed
        // from the user's keystrokes, or repeated bells carrying no new process output). out: must
        // not be pinned forward to each bell's wall-clock time.
        for (long nowMillis = 3_000L; nowMillis <= 60_000L; nowMillis += 1_000L) {
            recordBellOutputActivityIfGenuine(tracker, store, 101L, nowMillis);
        }

        Assert.assertEquals(Long.valueOf(2_000L), store.getLastOutputActivityTimeMillis(SESSION));
    }

    @Test
    public void aBellThatAccompaniesGenuineNewOutputAdvancesOutTimeOnce() {
        SessionOutputProgressTracker tracker = new SessionOutputProgressTracker();
        SessionNewActivityStore store = new SessionNewActivityStore();

        recordBellOutputActivityIfGenuine(tracker, store, 100L, 1_000L); // seed baseline
        recordBellOutputActivityIfGenuine(tracker, store, 105L, 5_000L); // genuine output: advances

        Assert.assertEquals(Long.valueOf(5_000L), store.getLastOutputActivityTimeMillis(SESSION));

        // Re-delivered bell at the same version does not advance again.
        recordBellOutputActivityIfGenuine(tracker, store, 105L, 6_000L);
        Assert.assertEquals(Long.valueOf(5_000L), store.getLastOutputActivityTimeMillis(SESSION));
    }
}
