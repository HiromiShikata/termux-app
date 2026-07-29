package com.termux.app.terminal.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionEagerLoadPacerTest {

    private static final class PendingMainThreadMessages
            implements SessionEagerLoadPacer.MainThreadMessagePoster {
        private final Deque<Runnable> messages = new ArrayDeque<>();
        final List<Long> postedDelaysMillis = new ArrayList<>();

        @Override
        public void postToMainThreadDelayed(@NonNull Runnable runnable, long delayMillis) {
            messages.add(runnable);
            postedDelaysMillis.add(delayMillis);
        }

        int getPendingMessageCount() {
            return messages.size();
        }

        boolean runNextMessage() {
            Runnable message = messages.poll();
            if (message == null) return false;
            message.run();
            return true;
        }

        int runUntilNoMessagesRemain() {
            int executedMessageCount = 0;
            while (runNextMessage()) executedMessageCount++;
            return executedMessageCount;
        }
    }

    private static final class RecordingInitializationAction
            implements SessionEagerLoader.SessionInitializationAction {
        final List<TerminalSession> initializedSessions = new ArrayList<>();

        @Override
        public void initializeSession(@NonNull TerminalSession session) {
            initializedSessions.add(session);
        }
    }

    private final PendingMainThreadMessages mainThreadMessages = new PendingMainThreadMessages();

    private final RecordingInitializationAction initializationAction =
        new RecordingInitializationAction();

    private TerminalSession newSessionToEagerLoad() {
        return new TerminalSession("/bin/sh", "/", new String[0], new String[0], null, null);
    }

    private SessionEagerLoadPacer newPacer() {
        return new SessionEagerLoadPacer(mainThreadMessages, initializationAction);
    }

    @Test
    public void schedulesTheFirstSessionBehindAFrameYieldInsteadOfRunningItImmediately() {
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(newSessionToEagerLoad());

        assertTrue(initializationAction.initializedSessions.isEmpty());
        assertEquals(
            List.of(SessionEagerLoadPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS),
            mainThreadMessages.postedDelaysMillis);
    }

    @Test
    public void schedulesOnlyOneMainThreadMessageHoweverManySessionsAreEnqueued() {
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(newSessionToEagerLoad());
        pacer.enqueueSession(newSessionToEagerLoad());
        pacer.enqueueSession(newSessionToEagerLoad());

        assertEquals(1, mainThreadMessages.getPendingMessageCount());
    }

    @Test
    public void initializesExactlyOneSessionPerMainThreadMessage() {
        TerminalSession first = newSessionToEagerLoad();
        TerminalSession second = newSessionToEagerLoad();
        TerminalSession third = newSessionToEagerLoad();
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(first);
        pacer.enqueueSession(second);
        pacer.enqueueSession(third);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first), initializationAction.initializedSessions);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first, second), initializationAction.initializedSessions);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first, second, third), initializationAction.initializedSessions);
    }

    @Test
    public void schedulesEverySuccessorSessionBehindItsOwnFrameYield() {
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(newSessionToEagerLoad());
        pacer.enqueueSession(newSessionToEagerLoad());
        pacer.enqueueSession(newSessionToEagerLoad());
        mainThreadMessages.runUntilNoMessagesRemain();

        long frameYield = SessionEagerLoadPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS;
        assertEquals(List.of(frameYield, frameYield, frameYield),
            mainThreadMessages.postedDelaysMillis);
    }

    @Test
    public void initializesEverySessionInEnqueueOrderOnceAllMessagesRun() {
        TerminalSession first = newSessionToEagerLoad();
        TerminalSession second = newSessionToEagerLoad();
        TerminalSession third = newSessionToEagerLoad();
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(first);
        pacer.enqueueSession(second);
        pacer.enqueueSession(third);

        assertEquals(3, mainThreadMessages.runUntilNoMessagesRemain());
        assertEquals(List.of(first, second, third), initializationAction.initializedSessions);
        assertEquals(0, mainThreadMessages.getPendingMessageCount());
    }

    @Test
    public void ignoresASessionThatIsAlreadyWaitingToBeInitialized() {
        TerminalSession session = newSessionToEagerLoad();
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(session);
        pacer.enqueueSession(session);
        pacer.enqueueSession(session);

        assertEquals(1, mainThreadMessages.runUntilNoMessagesRemain());
        assertEquals(List.of(session), initializationAction.initializedSessions);
    }

    @Test
    public void initializesASessionEnqueuedWhileAUnitIsRunningInALaterMessage() {
        TerminalSession first = newSessionToEagerLoad();
        TerminalSession enqueuedDuringFirstUnit = newSessionToEagerLoad();
        List<TerminalSession> initializedSessions = new ArrayList<>();
        SessionEagerLoadPacer[] pacerHolder = new SessionEagerLoadPacer[1];
        pacerHolder[0] = new SessionEagerLoadPacer(mainThreadMessages, session -> {
            initializedSessions.add(session);
            if (session == first) pacerHolder[0].enqueueSession(enqueuedDuringFirstUnit);
        });

        pacerHolder[0].enqueueSession(first);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first), initializedSessions);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first, enqueuedDuringFirstUnit), initializedSessions);
    }

    @Test
    public void initializesEveryRemainingSessionAfterOneSessionInitializationThrows() {
        TerminalSession failingSession = newSessionToEagerLoad();
        TerminalSession secondSession = newSessionToEagerLoad();
        TerminalSession thirdSession = newSessionToEagerLoad();
        List<TerminalSession> initializedSessions = new ArrayList<>();
        SessionEagerLoadPacer pacer = new SessionEagerLoadPacer(mainThreadMessages, session -> {
            initializedSessions.add(session);
            if (session == failingSession) {
                throw new IllegalStateException("session initialization failed");
            }
        });

        pacer.enqueueSession(failingSession);
        pacer.enqueueSession(secondSession);
        pacer.enqueueSession(thirdSession);

        IllegalStateException thrownByTheFailingUnit =
            assertThrows(IllegalStateException.class, mainThreadMessages::runNextMessage);

        assertEquals("a session initialization failure must stay visible to the caller rather than being "
                + "swallowed by the pacer",
            "session initialization failed", thrownByTheFailingUnit.getMessage());
        assertEquals("the unit that follows a throwing unit must already be scheduled when the throw "
                + "leaves the pacer, so the queue keeps draining without waiting for another foreground "
                + "transition to revive it",
            2, mainThreadMessages.runUntilNoMessagesRemain());
        assertEquals("one session whose process creation fails must not strand every session queued "
                + "behind it, which would leave the owner with a single initialized session and the rest "
                + "pending until some later enqueue happens to revive the pacer",
            List.of(failingSession, secondSession, thirdSession), initializedSessions);
    }

    @Test
    public void postsNoMainThreadMessageWhenNoSessionWasEnqueued() {
        newPacer();

        assertEquals(0, mainThreadMessages.getPendingMessageCount());
    }
}
