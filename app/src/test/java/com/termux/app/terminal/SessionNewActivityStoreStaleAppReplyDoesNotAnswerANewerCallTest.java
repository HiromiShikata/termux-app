package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStoreStaleAppReplyDoesNotAnswerANewerCallTest {

    private static final String SESSION = "session-the-owner-has-not-replied-to";
    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS = 1_000_000_000L;
    private static final long OWNER_CALL_ON_THE_HOST_CLOCK_MILLIS =
        OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS + (45L * ONE_SECOND_MILLIS);
    private static final long DEVICE_CLOCK_AHEAD_OF_THE_HOST_MILLIS = 2L * ONE_MINUTE_MILLIS;
    private static final long NOW_MILLIS =
        OWNER_CALL_ON_THE_HOST_CLOCK_MILLIS + (15L * ONE_MINUTE_MILLIS);

    @Test
    public void aCallNewerThanTheStatuslineReplyStaysRedWhenEveryAppReplyPredatesThatCall() {
        SessionNewActivityStore store = storeWhoseOwnerRepliedBeforeTheCallArrived();

        recordTheCallThatNobodyHasAnsweredYet(store);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION, NOW_MILLIS));
    }

    @Test
    public void theBottomSheetSessionListShowsTheSameRedDotForThatCall() {
        SessionNewActivityStore store = storeWhoseOwnerRepliedBeforeTheCallArrived();

        recordTheCallThatNobodyHasAnsweredYet(store);

        Assert.assertEquals(SessionNewActivityTier.RED,
            TermuxSessionsListViewController.newActivityIndicator(store, SESSION, NOW_MILLIS)
                .getTier());
    }

    @Test
    public void anAppReplyTypedAfterTheAppSawTheCallStillClearsTheRedDot() {
        SessionNewActivityStore store = storeWhoseOwnerRepliedBeforeTheCallArrived();
        recordTheCallThatNobodyHasAnsweredYet(store);

        store.recordGenuineAppReply(SESSION, NOW_MILLIS);

        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION, NOW_MILLIS));
    }

    @Test
    public void reparsingTheSameCallKeepsTheAppReplyThatAlreadyAnsweredIt() {
        SessionNewActivityStore store = storeWhoseOwnerRepliedBeforeTheCallArrived();
        recordTheCallThatNobodyHasAnsweredYet(store);
        store.recordGenuineAppReply(SESSION, NOW_MILLIS);

        store.recordStatuslineTimes(SESSION, OWNER_CALL_ON_THE_HOST_CLOCK_MILLIS,
            OWNER_CALL_ON_THE_HOST_CLOCK_MILLIS + ONE_MINUTE_MILLIS,
            OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS);

        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION, NOW_MILLIS));
    }

    private void recordTheCallThatNobodyHasAnsweredYet(SessionNewActivityStore store) {
        store.recordStatuslineTimes(SESSION, OWNER_CALL_ON_THE_HOST_CLOCK_MILLIS,
            OWNER_CALL_ON_THE_HOST_CLOCK_MILLIS, OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS);
    }

    private SessionNewActivityStore storeWhoseOwnerRepliedBeforeTheCallArrived() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION, null, OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS,
            OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS);
        store.recordGenuineAppReply(SESSION,
            OWNER_REPLY_ON_THE_HOST_CLOCK_MILLIS + DEVICE_CLOCK_AHEAD_OF_THE_HOST_MILLIS);
        return store;
    }
}
