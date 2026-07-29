package com.termux.app.terminal.session;

import static org.junit.Assert.assertEquals;
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

    private static final class MutableEagerLoadList
            implements SessionEagerLoader.SessionListSupplier {
        private final List<TerminalSession> sessions = new ArrayList<>();

        @NonNull
        @Override
        public List<TerminalSession> getSessionsToEagerLoad() {
            return sessions;
        }
    }

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

    private final MutableEagerLoadList eagerLoadList = new MutableEagerLoadList();

    private final PendingMainThreadMessages mainThreadMessages = new PendingMainThreadMessages();

    private final RecordingInitializationAction initializationAction =
        new RecordingInitializationAction();

    private TerminalSession newSessionToEagerLoad() {
        TerminalSession session =
            new TerminalSession("/bin/sh", "/", new String[0], new String[0], null, null);
        eagerLoadList.sessions.add(session);
        return session;
    }

    private SessionEagerLoadPacer newPacer() {
        return new SessionEagerLoadPacer(eagerLoadList, mainThreadMessages, initializationAction);
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
        assertEquals(0, pacer.getPendingSessionCount());
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
    public void skipsASessionThatLeftTheEagerLoadListBeforeItsMessageRan() {
        TerminalSession removedBeforeItsTurn = newSessionToEagerLoad();
        TerminalSession stillPresent = newSessionToEagerLoad();
        SessionEagerLoadPacer pacer = newPacer();

        pacer.enqueueSession(removedBeforeItsTurn);
        pacer.enqueueSession(stillPresent);
        eagerLoadList.sessions.remove(removedBeforeItsTurn);

        mainThreadMessages.runUntilNoMessagesRemain();

        assertEquals(List.of(stillPresent), initializationAction.initializedSessions);
    }

    @Test
    public void initializesASessionEnqueuedWhileAUnitIsRunningInALaterMessage() {
        TerminalSession first = newSessionToEagerLoad();
        TerminalSession enqueuedDuringFirstUnit = newSessionToEagerLoad();
        List<TerminalSession> initializedSessions = new ArrayList<>();
        SessionEagerLoadPacer[] pacerHolder = new SessionEagerLoadPacer[1];
        pacerHolder[0] = new SessionEagerLoadPacer(eagerLoadList, mainThreadMessages, session -> {
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
    public void postsNoMainThreadMessageWhenNoSessionWasEnqueued() {
        newPacer();

        assertEquals(0, mainThreadMessages.getPendingMessageCount());
    }
}
