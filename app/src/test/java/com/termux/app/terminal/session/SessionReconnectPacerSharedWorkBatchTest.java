package com.termux.app.terminal.session;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionReconnectReason;
import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionReconnectPacerSharedWorkBatchTest {

    private static final SessionReconnectReason ANY_REASON =
        SessionReconnectReason.SHELL_PROCESS_GONE_AT_THE_BACKGROUND_SCAN;

    private final List<String> whatHappenedInOrder = new ArrayList<>();

    private final Deque<Runnable> mainThreadMessages = new ArrayDeque<>();

    private final List<TerminalSession> sessionsThatLeftTheReconnectList = new ArrayList<>();

    private SessionReconnectPacer newPacer() {
        return new SessionReconnectPacer(
            (runnable, delayMillis) -> mainThreadMessages.add(runnable),
            session -> !sessionsThatLeftTheReconnectList.contains(session),
            session -> whatHappenedInOrder.add("reconnect " + session.mSessionName),
            () -> 0L,
            (reason, elapsedNanos, sessionsStillQueued) -> {
            },
            new SessionReconnectPacer.SharedCreationWorkBatch() {
                @Override
                public void begin() {
                    whatHappenedInOrder.add("begin");
                }

                @Override
                public void end() {
                    whatHappenedInOrder.add("end");
                }
            });
    }

    private static TerminalSession newDeadSessionNamed(@NonNull String name) {
        TerminalSession session =
            new TerminalSession("/bin/sh", "/", new String[0], new String[0], null, null);
        session.mSessionName = name;
        return session;
    }

    private void runUntilNoMainThreadMessagesRemain() {
        while (true) {
            Runnable message = mainThreadMessages.poll();
            if (message == null) return;
            message.run();
        }
    }

    @Test
    public void aSweepOfSeveralSessionsPublishesTheWorkSharedByCreatedSessionsOnceForTheWholeSweep() {
        SessionReconnectPacer pacer = newPacer();
        pacer.enqueueSession(newDeadSessionNamed("first"), ANY_REASON);
        pacer.enqueueSession(newDeadSessionNamed("second"), ANY_REASON);
        pacer.enqueueSession(newDeadSessionNamed("third"), ANY_REASON);

        runUntilNoMainThreadMessagesRemain();

        assertEquals("the sessions-list update, the wake lock reconciliation and the notification"
                + " update are what every created session shares, so reconnecting three sessions has to"
                + " issue them once for the sweep rather than once per session",
            List.of("begin", "reconnect first", "reconnect second", "reconnect third", "end"),
            whatHappenedInOrder);
    }

    @Test
    public void aReconnectThatHappensOnItsOwnStillPublishesThatWorkImmediately() {
        SessionReconnectPacer pacer = newPacer();
        pacer.enqueueSession(newDeadSessionNamed("only"), ANY_REASON);

        runUntilNoMainThreadMessagesRemain();

        assertEquals("a single reconnect has nothing to be batched with, so holding its shared work"
                + " back would leave the list, the wake lock and the notification stale until some later"
                + " sweep happened to close a batch",
            List.of("begin", "reconnect only", "end"), whatHappenedInOrder);
    }

    @Test
    public void aSessionQueuedWhileTheSweepIsRunningIsCoveredByTheSameOpenBatch() {
        SessionReconnectPacer pacer = newPacer();
        pacer.enqueueSession(newDeadSessionNamed("first"), ANY_REASON);

        Runnable firstUnit = mainThreadMessages.poll();
        pacer.enqueueSession(newDeadSessionNamed("second"), ANY_REASON);
        firstUnit.run();
        runUntilNoMainThreadMessagesRemain();

        assertEquals("a session found dead while the sweep is already draining belongs to the same"
                + " sweep, so it must not close the batch early and pay the shared work twice",
            List.of("begin", "reconnect first", "reconnect second", "end"), whatHappenedInOrder);
    }

    @Test
    public void anOpenBatchIsClosedEvenWhenTheLastQueuedSessionHasLeftTheReconnectList() {
        SessionReconnectPacer pacer = newPacer();
        TerminalSession sessionThatWillLeave = newDeadSessionNamed("leaving");
        pacer.enqueueSession(newDeadSessionNamed("first"), ANY_REASON);
        pacer.enqueueSession(sessionThatWillLeave, ANY_REASON);
        sessionsThatLeftTheReconnectList.add(sessionThatWillLeave);

        runUntilNoMainThreadMessagesRemain();

        assertEquals("a batch that stays open publishes nothing, so the last queued session turning"
                + " out to need no reconnect must still close it rather than leave the list, the wake lock"
                + " and the notification waiting for some later sweep",
            List.of("begin", "reconnect first", "end"), whatHappenedInOrder);
    }
}
