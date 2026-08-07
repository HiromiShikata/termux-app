package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class GenuineAppReplySurvivesRestartTest {

    private static final String SESSION_NAME = "session-one";
    private static final long STATUSLINE_REPLY_TIME_MILLIS = 1_000_000L;
    private static final long CALL_TIME_MILLIS = 1_100_000L;
    private static final long IN_APP_REPLY_TIME_MILLIS = 1_150_000L;
    private static final long NOW_MILLIS = 1_200_000L;

    private SessionNewActivityStore storeWithACallTheOwnerAnsweredInTheApp(
        InMemorySessionNewActivityPersistence persistence) {
        SessionNewActivityStore store = new SessionNewActivityStore(persistence);
        store.recordStatuslineTimes(SESSION_NAME, CALL_TIME_MILLIS, CALL_TIME_MILLIS,
            STATUSLINE_REPLY_TIME_MILLIS, 0);
        store.recordGenuineAppReply(SESSION_NAME, IN_APP_REPLY_TIME_MILLIS);
        return store;
    }

    @Test
    public void theInAppReplyClearsThePendingCallBeforeAnyRestart() {
        SessionNewActivityStore store =
            storeWithACallTheOwnerAnsweredInTheApp(new InMemorySessionNewActivityPersistence());

        Assert.assertNull(store.statuslineCallPendingTimeMillis(SESSION_NAME));
    }

    @Test
    public void theInAppReplySubmittedBeforeARestartStillClearsThePendingCallAfterIt() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        storeWithACallTheOwnerAnsweredInTheApp(persistence);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertNull("the owner already replied in the app, so the reply time must survive the "
                + "restart instead of falling back to the laggy statusline reply token and arming the "
                + "red dot again for a call the owner has answered",
            afterRestart.statuslineCallPendingTimeMillis(SESSION_NAME));
    }

    @Test
    public void theRestoredInAppReplyIsTheGenuineReplyTimeAfterARestart() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        storeWithACallTheOwnerAnsweredInTheApp(persistence);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertEquals(Long.valueOf(IN_APP_REPLY_TIME_MILLIS),
            afterRestart.genuineReplyTimeMillis(SESSION_NAME));
    }

    @Test
    public void theSessionIsNotRedAfterTheRestartThatRestoredTheInAppReply() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        storeWithACallTheOwnerAnsweredInTheApp(persistence);

        SessionNewActivityStore afterRestart = new SessionNewActivityStore(persistence);

        Assert.assertNotEquals(SessionNewActivityTier.RED,
            afterRestart.tierFor(SESSION_NAME, NOW_MILLIS));
    }
}
