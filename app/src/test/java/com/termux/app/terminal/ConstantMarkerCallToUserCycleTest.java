package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.termux.app.apkupdate.UpdateTagUpdateController;

import org.junit.Test;

public class ConstantMarkerCallToUserCycleTest {

    private static final String SESSION = "worker";
    private static final String HANDLE = "handle-1";
    private static final String MARKER = "🔴";
    private static final String TAG_BLOCK = "<call-to-user>" + MARKER + "</call-to-user>\n";
    private static final long BASE_CALL_SECONDS = 36000L;

    private final SessionNewActivityStore store = new SessionNewActivityStore();

    private long tagScanTimeMillis;

    private final CallToUserTagController controller = new CallToUserTagController(
        (sessionKey, reason, callCycleKey) ->
            store.recordExplicitCall(SESSION, tagScanTimeMillis, reason, callCycleKey));

    private static String clock(long seconds) {
        return String.format("%02d:%02d:%02d", (seconds / 3600) % 24, (seconds / 60) % 60,
            seconds % 60);
    }

    private static String statuslineLine(long callSeconds, long replySeconds) {
        return "[claude] out:" + clock(callSeconds) + " call:" + clock(callSeconds)
            + " reply:" + clock(replySeconds) + " SUB:0\n";
    }

    private boolean ownerIsPaged() {
        return store.pendingCallToUserSessionCount() > 0
            && !store.getUnacknowledgedCallReasons(SESSION).isEmpty();
    }

    private int pagesAcrossCycles(int cycleCount, boolean retainOlderTags) {
        int pages = 0;
        StringBuilder retainedWindow = new StringBuilder();
        long previousReplySeconds = BASE_CALL_SECONDS - 60L;
        for (int cycle = 1; cycle <= cycleCount; cycle++) {
            long callSeconds = BASE_CALL_SECONDS + cycle * 60L;
            long replySeconds = callSeconds + 30L;
            tagScanTimeMillis = callSeconds * 1000L;

            store.recordStatuslineTimes(SESSION, callSeconds * 1000L, callSeconds * 1000L,
                previousReplySeconds * 1000L, 0);

            String cycleOutput = "work line " + cycle + "\n"
                + statuslineLine(callSeconds, previousReplySeconds) + TAG_BLOCK;
            retainedWindow.append(cycleOutput);

            if (store.shouldScanCallToUserTag(SESSION)) {
                controller.onSessionTextChanged(HANDLE,
                    retainOlderTags ? retainedWindow.toString() : cycleOutput);
                store.recordCallToUserTagScanPerformed(SESSION);
            }
            if (ownerIsPaged()) {
                pages++;
            }

            store.recordGenuineAppReply(SESSION, replySeconds * 1000L);
            store.recordStatuslineTimes(SESSION, callSeconds * 1000L, callSeconds * 1000L,
                replySeconds * 1000L, 0);
            previousReplySeconds = replySeconds;
        }
        return pages;
    }

    @Test
    public void twoConstantMarkerCallsSeparatedByAnOwnerReplyBothPageTheOwner() {
        assertEquals(2, pagesAcrossCycles(2, false));
    }

    @Test
    public void fiveConstantMarkerCallsPageTheOwnerEveryTimeWhenOlderTagsScrollOffTheWindow() {
        assertEquals(5, pagesAcrossCycles(5, false));
    }

    @Test
    public void fiveConstantMarkerCallsPageTheOwnerEveryTimeWhenEveryOlderTagStaysInTheWindow() {
        assertEquals(5, pagesAcrossCycles(5, true));
    }

    @Test
    public void aSingleOutputContainingTheSameCompleteTagTwicePagesTheOwnerOnce() {
        tagScanTimeMillis = (BASE_CALL_SECONDS + 60L) * 1000L;
        store.recordStatuslineTimes(SESSION, tagScanTimeMillis, tagScanTimeMillis,
            BASE_CALL_SECONDS * 1000L, 0);

        controller.onSessionTextChanged(HANDLE,
            statuslineLine(BASE_CALL_SECONDS + 60L, BASE_CALL_SECONDS) + TAG_BLOCK + TAG_BLOCK);

        assertEquals(1, store.getUnacknowledgedCallReasons(SESSION).size());
    }

    @Test
    public void reScanningAnUnchangedWindowAfterTheOwnerRepliedDoesNotPageTheOwnerAgain() {
        long callSeconds = BASE_CALL_SECONDS + 60L;
        tagScanTimeMillis = callSeconds * 1000L;
        String window = statuslineLine(callSeconds, BASE_CALL_SECONDS) + TAG_BLOCK;

        store.recordStatuslineTimes(SESSION, callSeconds * 1000L, callSeconds * 1000L,
            BASE_CALL_SECONDS * 1000L, 0);
        controller.onSessionTextChanged(HANDLE, window);
        assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));

        store.recordGenuineAppReply(SESSION, (callSeconds + 30L) * 1000L);
        store.recordStatuslineTimes(SESSION, callSeconds * 1000L, callSeconds * 1000L,
            (callSeconds + 30L) * 1000L, 0);
        assertFalse(ownerIsPaged());

        tagScanTimeMillis = (callSeconds + 600L) * 1000L;
        controller.onSessionTextChanged(HANDLE, window);
        controller.onSessionTextChanged(HANDLE, window);

        assertFalse(ownerIsPaged());
        assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        assertEquals(Long.valueOf(callSeconds * 1000L),
            store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void aBackgroundedScanDistinguishesCyclesWithoutAnyStatuslineTimesBeingRecorded() {
        BackgroundOutputTagScanner backgroundScanner =
            new BackgroundOutputTagScanner(controller, new UpdateTagUpdateController(reason -> {
            }));

        long firstCallSeconds = BASE_CALL_SECONDS + 60L;
        tagScanTimeMillis = firstCallSeconds * 1000L;
        backgroundScanner.scan(HANDLE,
            statuslineLine(firstCallSeconds, BASE_CALL_SECONDS) + TAG_BLOCK, true);
        assertEquals(1, store.getUnacknowledgedCallReasons(SESSION).size());

        store.recordUserInput(SESSION, (firstCallSeconds + 30L) * 1000L);
        assertEquals(0, store.getUnacknowledgedCallReasons(SESSION).size());

        long secondCallSeconds = firstCallSeconds + 120L;
        tagScanTimeMillis = secondCallSeconds * 1000L;
        backgroundScanner.scan(HANDLE,
            statuslineLine(secondCallSeconds, firstCallSeconds + 30L) + TAG_BLOCK, true);

        assertEquals(1, store.getUnacknowledgedCallReasons(SESSION).size());
        assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
    }
}
