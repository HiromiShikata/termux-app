package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.termux.app.apkupdate.UpdateTagUpdateController;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Verifies that the explicit-call and app-update output tags are detected for every session that
 * produces output, regardless of which session is currently viewed or whether the app is
 * foregrounded, while the open-URL tag stays out of this background scan path.
 */
public class BackgroundOutputTagScannerTest {

    private static final String CALLED_SESSION = "called-session";
    private static final String OTHER_SESSION = "other-session";

    private static final class RecordingUpdateTrigger implements UpdateTagUpdateController.ReasonTrigger {
        final List<String> reasons = new ArrayList<>();

        @Override
        public void onUpdateRequested(String reason) {
            reasons.add(reason);
        }
    }

    private static BackgroundOutputTagScanner scannerRecordingInto(
            SessionNewActivityStore store, UpdateTagUpdateController.ReasonTrigger updateTrigger) {
        CallToUserTagController callToUserTagController = new CallToUserTagController(
            (sessionKey, reason) -> store.recordExplicitCall(sessionKey, System.currentTimeMillis(), reason));
        UpdateTagUpdateController updateTagUpdateController = new UpdateTagUpdateController(updateTrigger);
        return new BackgroundOutputTagScanner(callToUserTagController, updateTagUpdateController);
    }

    @Test
    public void nonCurrentSessionCallToUserTagRecordsRedTierExplicitCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        BackgroundOutputTagScanner scanner = scannerRecordingInto(store, updateTrigger);

        // The called session is not the current/viewed session: the scanner is fed its handle
        // directly, exactly as the service client and the activity client now do for every session.
        scanner.scan(CALLED_SESSION, "running <call-to-user>needs approval</call-to-user>");

        assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        assertEquals("needs approval", store.getLastExplicitCallReason(CALLED_SESSION));
        assertTrue(updateTrigger.reasons.isEmpty());
    }

    @Test
    public void backgroundSessionUpdateTagTriggersUpdateFlow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        BackgroundOutputTagScanner scanner = scannerRecordingInto(store, updateTrigger);

        scanner.scan(OTHER_SESSION, "log <update-termux-app>new release</update-termux-app>");

        assertEquals(1, updateTrigger.reasons.size());
        assertEquals("new release", updateTrigger.reasons.get(0));
        // The update tag is not an explicit call, so no red tier is recorded for it.
        assertEquals(SessionNewActivityTier.NONE, store.tierFor(OTHER_SESSION));
    }

    @Test
    public void openUrlTagIsNotActedOnByTheBackgroundScanner() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        BackgroundOutputTagScanner scanner = scannerRecordingInto(store, updateTrigger);

        // An open-URL tag in a non-current session must not record an explicit call or trigger an
        // update; auto-open stays current-session-only and is handled elsewhere by the activity.
        scanner.scan(OTHER_SESSION, "see <open>https://example.com</open>");

        assertEquals(SessionNewActivityTier.NONE, store.tierFor(OTHER_SESSION));
        assertNull(store.getLastExplicitCallTimeMillis(OTHER_SESSION));
        assertTrue(updateTrigger.reasons.isEmpty());
    }

    @Test
    public void callToUserTagFiresOncePerOccurrenceOnRepeatedScans() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        CallToUserTagController callToUserTagController = new CallToUserTagController(
            (sessionKey, reason) -> store.recordExplicitCall(sessionKey, System.currentTimeMillis(), reason));
        BackgroundOutputTagScanner scanner =
            new BackgroundOutputTagScanner(callToUserTagController, new UpdateTagUpdateController(updateTrigger));

        String transcript = "<call-to-user>approval</call-to-user>";
        scanner.scan(CALLED_SESSION, transcript);
        Long firstCallTime = store.getLastExplicitCallTimeMillis(CALLED_SESSION);

        // Re-scanning a transcript that still contains the already-fired tag must not re-fire it.
        scanner.scan(CALLED_SESSION, transcript + "\nfollow-up line 1");
        scanner.scan(CALLED_SESSION, transcript + "\nfollow-up line 1\nfollow-up line 2");

        assertEquals(firstCallTime, store.getLastExplicitCallTimeMillis(CALLED_SESSION));
    }

    @Test
    public void updateTagFiresOncePerReasonOnRepeatedScans() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        BackgroundOutputTagScanner scanner = scannerRecordingInto(store, updateTrigger);

        String transcript = "<update-termux-app>release one</update-termux-app>";
        scanner.scan(CALLED_SESSION, transcript);
        scanner.scan(CALLED_SESSION, transcript + "\nmore output");

        assertEquals(1, updateTrigger.reasons.size());
    }

    @Test
    public void deduplicationIsPerSessionIndependently() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        BackgroundOutputTagScanner scanner = scannerRecordingInto(store, updateTrigger);

        String transcript = "<call-to-user>shared reason</call-to-user>";
        scanner.scan(CALLED_SESSION, transcript);
        scanner.scan(OTHER_SESSION, transcript);

        assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        assertEquals(SessionNewActivityTier.RED, store.tierFor(OTHER_SESSION));
    }

    @Test
    public void ignoresNullSessionKeyAndNullTranscript() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingUpdateTrigger updateTrigger = new RecordingUpdateTrigger();
        BackgroundOutputTagScanner scanner = scannerRecordingInto(store, updateTrigger);

        scanner.scan(null, "<call-to-user>reason</call-to-user>");
        scanner.scan(CALLED_SESSION, null);

        assertTrue(updateTrigger.reasons.isEmpty());
        assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }
}
