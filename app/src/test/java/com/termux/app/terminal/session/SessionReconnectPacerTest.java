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
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SessionReconnectPacerTest {

    private static final class PendingMainThreadMessages
            implements SessionReconnectPacer.MainThreadMessagePoster {

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

    private static final long NANOS_EVERY_RECONNECT_SPENDS = 3_000_000L;

    private static final class AdvancingNanoClock implements SessionReconnectPacer.ElapsedNanosClock {

        private final long advancePerReadingNanos;

        private long currentNanos;

        AdvancingNanoClock(long advancePerReadingNanos) {
            this.advancePerReadingNanos = advancePerReadingNanos;
        }

        @Override
        public long elapsedNanos() {
            long reading = currentNanos;
            currentNanos += advancePerReadingNanos;
            return reading;
        }
    }

    private static final class RecordedReconnectCosts
            implements SessionReconnectPacer.ReconnectCostRecorder {

        final List<Long> elapsedNanos = new ArrayList<>();

        final List<Integer> sessionsStillQueued = new ArrayList<>();

        @Override
        public void recordReconnectCost(long elapsedNanos, int sessionsStillQueued) {
            this.elapsedNanos.add(elapsedNanos);
            this.sessionsStillQueued.add(sessionsStillQueued);
        }
    }

    private final PendingMainThreadMessages mainThreadMessages = new PendingMainThreadMessages();

    private final AdvancingNanoClock mainThreadNanoClock =
        new AdvancingNanoClock(NANOS_EVERY_RECONNECT_SPENDS);

    private final RecordedReconnectCosts recordedReconnectCosts = new RecordedReconnectCosts();

    private final List<TerminalSession> reconnectedSessions = new ArrayList<>();

    private final Set<TerminalSession> sessionsThatLeftTheReconnectList =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private TerminalSession newDeadSession() {
        return new TerminalSession("/bin/sh", "/", new String[0], new String[0], null, null);
    }

    private SessionReconnectPacer newPacer() {
        return newPacerReconnectingWith(reconnectedSessions::add);
    }

    private SessionReconnectPacer newPacerReconnectingWith(
            @NonNull SessionReconnectPacer.SessionReconnectAction sessionReconnectAction) {
        return new SessionReconnectPacer(mainThreadMessages,
            session -> !sessionsThatLeftTheReconnectList.contains(session), sessionReconnectAction,
            mainThreadNanoClock, recordedReconnectCosts);
    }

    @Test
    public void schedulesTheFirstSessionBehindAFrameYieldInsteadOfReconnectingItImmediately() {
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(newDeadSession());

        assertTrue("a session creation that runs inside the scheduling pass holds the main thread before "
                + "the drawing thread has had a single frame, which is what leaves the terminal screen on "
                + "its previous frame or blank",
            reconnectedSessions.isEmpty());
        assertEquals(List.of(SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS),
            mainThreadMessages.postedDelaysMillis);
    }

    @Test
    public void schedulesOnlyOneMainThreadMessageHoweverManySessionsAreEnqueued() {
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());

        assertEquals(1, mainThreadMessages.getPendingMessageCount());
    }

    @Test
    public void reconnectsExactlyOneSessionPerMainThreadMessage() {
        TerminalSession first = newDeadSession();
        TerminalSession second = newDeadSession();
        TerminalSession third = newDeadSession();
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(first);
        pacer.enqueueSession(second);
        pacer.enqueueSession(third);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first), reconnectedSessions);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first, second), reconnectedSessions);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first, second, third), reconnectedSessions);
    }

    @Test
    public void schedulesEverySuccessorSessionBehindItsOwnFrameYield() {
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        mainThreadMessages.runUntilNoMessagesRemain();

        long frameYield = SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS;
        assertEquals(List.of(frameYield, frameYield, frameYield),
            mainThreadMessages.postedDelaysMillis);
    }

    @Test
    public void reconnectsEverySessionInEnqueueOrderOnceAllMessagesRun() {
        TerminalSession first = newDeadSession();
        TerminalSession second = newDeadSession();
        TerminalSession third = newDeadSession();
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(first);
        pacer.enqueueSession(second);
        pacer.enqueueSession(third);

        assertEquals(3, mainThreadMessages.runUntilNoMessagesRemain());
        assertEquals(List.of(first, second, third), reconnectedSessions);
        assertEquals(0, mainThreadMessages.getPendingMessageCount());
    }

    @Test
    public void ignoresASessionThatIsAlreadyWaitingToBeReconnected() {
        TerminalSession session = newDeadSession();
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(session);
        pacer.enqueueSession(session);
        pacer.enqueueSession(session);

        assertEquals(1, mainThreadMessages.runUntilNoMessagesRemain());
        assertEquals(List.of(session), reconnectedSessions);
    }

    @Test
    public void reconnectsASessionEnqueuedWhileAUnitIsRunningInALaterMessage() {
        TerminalSession first = newDeadSession();
        TerminalSession enqueuedDuringFirstUnit = newDeadSession();
        SessionReconnectPacer[] pacerHolder = new SessionReconnectPacer[1];
        pacerHolder[0] = newPacerReconnectingWith(session -> {
            reconnectedSessions.add(session);
            if (session == first) pacerHolder[0].enqueueSession(enqueuedDuringFirstUnit);
        });

        pacerHolder[0].enqueueSession(first);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first), reconnectedSessions);

        mainThreadMessages.runNextMessage();
        assertEquals(List.of(first, enqueuedDuringFirstUnit), reconnectedSessions);
    }

    @Test
    public void reconnectsEveryRemainingSessionAfterOneSessionCreationThrows() {
        TerminalSession failingSession = newDeadSession();
        TerminalSession secondSession = newDeadSession();
        TerminalSession thirdSession = newDeadSession();
        SessionReconnectPacer pacer = newPacerReconnectingWith(session -> {
            reconnectedSessions.add(session);
            if (session == failingSession) {
                throw new IllegalStateException("session creation failed");
            }
        });

        pacer.enqueueSession(failingSession);
        pacer.enqueueSession(secondSession);
        pacer.enqueueSession(thirdSession);

        IllegalStateException thrownByTheFailingUnit =
            assertThrows(IllegalStateException.class, mainThreadMessages::runNextMessage);

        assertEquals("a session creation failure must stay visible to the caller rather than being "
                + "swallowed by the pacer",
            "session creation failed", thrownByTheFailingUnit.getMessage());
        assertEquals("the unit that follows a throwing unit must already be scheduled when the throw "
                + "leaves the pacer, so the queue keeps draining without waiting for another sweep to "
                + "revive it",
            2, mainThreadMessages.runUntilNoMessagesRemain());
        assertEquals("one session whose process creation fails must not strand every session queued "
                + "behind it, which would leave the owner with a single reconnected session and the rest "
                + "pending until some later sweep happens to revive the pacer",
            List.of(failingSession, secondSession, thirdSession), reconnectedSessions);
    }

    @Test
    public void doesNotReconnectASessionThatLeftTheReconnectListBeforeItsMessageRan() {
        TerminalSession sessionLeavingTheList = newDeadSession();
        TerminalSession sessionQueuedBehindIt = newDeadSession();
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(sessionLeavingTheList);
        pacer.enqueueSession(sessionQueuedBehindIt);
        sessionsThatLeftTheReconnectList.add(sessionLeavingTheList);

        mainThreadMessages.runUntilNoMessagesRemain();

        assertEquals("pacing opens a window in which the session list changes under the pacer, so a "
                + "session closed or removed after it was enqueued must not have a replacement shell "
                + "forked for it, while every session still in the list must keep draining",
            List.of(sessionQueuedBehindIt), reconnectedSessions);
    }

    @Test
    public void recordsWhatEveryReconnectCostTheMainThreadSoASubThresholdBurstStaysMeasurable() {
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        mainThreadMessages.runUntilNoMessagesRemain();

        assertEquals("the stall watchdog only records a stall once the main thread has been blocked "
                + "past its threshold, so a run of reconnects that each stay under it leaves the "
                + "interface unresponsive while the report shows nothing unless every reconnect "
                + "contributes its own measurement",
            List.of(NANOS_EVERY_RECONNECT_SPENDS, NANOS_EVERY_RECONNECT_SPENDS,
                NANOS_EVERY_RECONNECT_SPENDS),
            recordedReconnectCosts.elapsedNanos);
    }

    @Test
    public void recordsHowManySessionsWereStillQueuedBehindEachReconnect() {
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        pacer.enqueueSession(newDeadSession());
        mainThreadMessages.runUntilNoMessagesRemain();

        assertEquals("a single slow reconnect and a burst that occupies the main thread once per "
                + "session read the same in a total, so the queue depth behind each reconnect is what "
                + "separates them",
            List.of(2, 1, 0), recordedReconnectCosts.sessionsStillQueued);
    }

    @Test
    public void recordsTheCostOfAReconnectThatThrewBecauseTheMainThreadStillSpentThatTime() {
        TerminalSession failingSession = newDeadSession();
        SessionReconnectPacer pacer = newPacerReconnectingWith(session -> {
            reconnectedSessions.add(session);
            throw new IllegalStateException("session creation failed");
        });

        pacer.enqueueSession(failingSession);
        assertThrows(IllegalStateException.class, mainThreadMessages::runNextMessage);

        assertEquals("a reconnect that fails has already spent its main-thread time, so dropping its "
                + "measurement would under-report exactly the case where reconnects are failing and "
                + "being retried",
            List.of(NANOS_EVERY_RECONNECT_SPENDS), recordedReconnectCosts.elapsedNanos);
    }

    @Test
    public void recordsNothingForASessionThatLeftTheReconnectListBeforeItsMessageRan() {
        TerminalSession sessionLeavingTheList = newDeadSession();
        SessionReconnectPacer pacer = newPacer();

        pacer.enqueueSession(sessionLeavingTheList);
        sessionsThatLeftTheReconnectList.add(sessionLeavingTheList);
        mainThreadMessages.runUntilNoMessagesRemain();

        assertEquals("no reconnect ran, so recording a cost for it would inflate the count the report "
                + "shows and invent main-thread time that was never spent",
            List.of(), recordedReconnectCosts.elapsedNanos);
    }

    @Test
    public void postsNoMainThreadMessageWhenNoSessionWasEnqueued() {
        newPacer();

        assertEquals(0, mainThreadMessages.getPendingMessageCount());
    }
}
