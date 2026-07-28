package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.app.terminal.session.SessionNewActivityStateSerializer;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

/**
 * The persisted call deduplication key was renamed from the legacy {@code acknowledgedCallReasons}
 * entry to {@code callTriggerValues}. A device that updates the app reads a document the previous
 * version wrote, and the first transcript scan after the update re-detects every owner-call tag
 * still sitting in a session's scrollback, so a lost key set re-arms answered calls and appends a
 * second copy of a pending call's reason. Under the replaced contract the reason text WAS the
 * deduplication key, so the legacy reasons seed the trigger values exactly.
 */
@RunWith(RobolectricTestRunner.class)
public class CallToUserLegacyStateMigrationTest {

    private static final String SESSION = "session-one";

    private static final String TRANSCRIPT_WITH_LEGACY_CALL =
        "running <call-to-user>needs approval</call-to-user>";

    private static final String ANSWERED_LEGACY_DOCUMENT =
        "[{\"sessionName\":\"session-one\",\"lastExplicitCallTimeMillis\":1000,"
            + "\"lastExplicitCallReason\":\"needs approval\","
            + "\"lastUserInputTimeMillis\":2000,"
            + "\"acknowledgedCallReasons\":[\"needs approval\"]}]";

    private static final String PENDING_LEGACY_DOCUMENT =
        "[{\"sessionName\":\"session-one\",\"lastExplicitCallTimeMillis\":1000,"
            + "\"lastExplicitCallReason\":\"needs approval\","
            + "\"unacknowledgedCallReasons\":[\"needs approval\"]}]";

    private static CallToUserTagController reloadControllerInto(SessionNewActivityStore store,
                                                                long fixedTimeMillis) {
        return new CallToUserTagController((sessionKey, triggerValue, reason) ->
            store.recordExplicitCall(sessionKey, fixedTimeMillis, triggerValue, reason));
    }

    private static SessionNewActivityStore storeLoadedFrom(String oldFormatDocument) {
        return new SessionNewActivityStore(new SessionNewActivityPersistence() {
            @NonNull
            @Override
            public List<SessionNewActivityState> load() {
                try {
                    return new SessionNewActivityStateSerializer().deserialize(oldFormatDocument);
                } catch (JSONException error) {
                    throw new AssertionError(error);
                }
            }

            @Override
            public void save(@NonNull List<SessionNewActivityState> states) {
            }
        });
    }

    @Test
    public void anAnsweredLegacyCallDoesNotReArmAfterLoadingTheOldFormat() {
        SessionNewActivityStore store = storeLoadedFrom(ANSWERED_LEGACY_DOCUMENT);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));

        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION, TRANSCRIPT_WITH_LEGACY_CALL);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
        Assert.assertTrue(store.getUnacknowledgedCallReasons(SESSION).isEmpty());
        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void aPendingLegacyCallReasonIsNotAppendedTwiceAfterLoadingTheOldFormat() {
        SessionNewActivityStore store = storeLoadedFrom(PENDING_LEGACY_DOCUMENT);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals(Arrays.asList("needs approval"),
            store.getUnacknowledgedCallReasons(SESSION));

        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION, TRANSCRIPT_WITH_LEGACY_CALL);

        Assert.assertEquals(Arrays.asList("needs approval"),
            store.getUnacknowledgedCallReasons(SESSION));
        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    /**
     * Bytes produced by the previous version's own serializer for a session with six answered and six
     * pending calls. Each legacy list is independently capped at {@code MAX_REASONS_PER_SESSION}, so
     * their union is twice that cap and exercises the over-cap boundary of the single replacement list.
     */
    private static final String SIX_ANSWERED_SIX_PENDING_LEGACY_DOCUMENT =
        "[{\"sessionName\":\"session-one\",\"lastExplicitCallTimeMillis\":6005,"
            + "\"lastExplicitCallReason\":\"pending 5\","
            + "\"unacknowledgedCallReasons\":[\"pending 0\",\"pending 1\",\"pending 2\","
            + "\"pending 3\",\"pending 4\",\"pending 5\"],"
            + "\"acknowledgedCallReasons\":[\"answered 0\",\"answered 1\",\"answered 2\","
            + "\"answered 3\",\"answered 4\",\"answered 5\"],"
            + "\"lastUserInputTimeMillis\":5000}]";

    private static final String TEN_ANSWERED_ONE_PENDING_LEGACY_DOCUMENT =
        "[{\"sessionName\":\"session-one\",\"lastExplicitCallTimeMillis\":6000,"
            + "\"lastExplicitCallReason\":\"pending 0\","
            + "\"unacknowledgedCallReasons\":[\"pending 0\"],"
            + "\"acknowledgedCallReasons\":[\"answered 0\",\"answered 1\",\"answered 2\","
            + "\"answered 3\",\"answered 4\",\"answered 5\",\"answered 6\",\"answered 7\","
            + "\"answered 8\",\"answered 9\"],"
            + "\"lastUserInputTimeMillis\":5000}]";

    private static String transcriptOf(int answeredCount, int pendingCount) {
        StringBuilder transcript = new StringBuilder();
        for (int index = 0; index < answeredCount; index++) {
            transcript.append("<call-to-user>answered ").append(index).append("</call-to-user>\n");
        }
        for (int index = 0; index < pendingCount; index++) {
            transcript.append("<call-to-user>pending ").append(index).append("</call-to-user>\n");
        }
        return transcript.toString();
    }

    private static void assertNoAnsweredCallReArms(SessionNewActivityStore store,
                                                   int answeredCount, int pendingCount) {
        List<String> pendingReasons = store.getUnacknowledgedCallReasons(SESSION);
        for (int index = 0; index < answeredCount; index++) {
            Assert.assertFalse("answered " + index + " must not re-arm",
                pendingReasons.contains("answered " + index));
        }
        for (int index = 0; index < pendingCount; index++) {
            int occurrences = 0;
            for (String reason : pendingReasons) {
                if (("pending " + index).equals(reason)) {
                    occurrences++;
                }
            }
            Assert.assertTrue("pending " + index + " must not be duplicated", occurrences <= 1);
        }
    }

    @Test
    public void noAnsweredCallReArmsWhenTheLegacyKeySetExceedsTheReasonCap() {
        SessionNewActivityStore store = storeLoadedFrom(SIX_ANSWERED_SIX_PENDING_LEGACY_DOCUMENT);
        String transcript = transcriptOf(6, 6);

        reloadControllerInto(store, 9_000_000L).onSessionTextChanged(SESSION, transcript);
        reloadControllerInto(store, 9_000_000L).onSessionTextChanged(SESSION, transcript);

        assertNoAnsweredCallReArms(store, 6, 6);
        Assert.assertEquals(Long.valueOf(6_005L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void noAnsweredCallReArmsWhenTheLegacyAcknowledgedListIsAtItsCap() {
        SessionNewActivityStore store = storeLoadedFrom(TEN_ANSWERED_ONE_PENDING_LEGACY_DOCUMENT);
        String transcript = transcriptOf(10, 1);

        reloadControllerInto(store, 9_000_000L).onSessionTextChanged(SESSION, transcript);
        reloadControllerInto(store, 9_000_000L).onSessionTextChanged(SESSION, transcript);

        assertNoAnsweredCallReArms(store, 10, 1);
        Assert.assertEquals(Long.valueOf(6_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void aGenuinelyNewCallStillFiresAfterLoadingTheOldFormat() {
        SessionNewActivityStore store = storeLoadedFrom(ANSWERED_LEGACY_DOCUMENT);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));

        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION,
            TRANSCRIPT_WITH_LEGACY_CALL + "\n<call-to-user>second approval</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals(Arrays.asList("second approval"),
            store.getUnacknowledgedCallReasons(SESSION));
        Assert.assertEquals(Long.valueOf(9_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }
}
