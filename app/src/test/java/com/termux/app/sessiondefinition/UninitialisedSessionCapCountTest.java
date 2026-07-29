package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class UninitialisedSessionCapCountTest {

    private static TerminalSession sessionWithoutEmulatorInitialisation(String sessionName) {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
        return terminalSession;
    }

    @Test
    public void aSessionThatNeverInitialisedAnEmulatorMustNotReportItselfAsRunning() {
        TerminalSession neverInitialisedSession = sessionWithoutEmulatorInitialisation("session-alpha");

        assertFalse("a session whose emulator was never initialised has no shell process, so it must "
                + "not report itself as running; its process identifier field defaults to 0 while the "
                + "running predicate only excludes -1",
            neverInitialisedSession.isRunning());
    }

    @Test
    public void aSessionThatNeverInitialisedAnEmulatorMustNotCountTowardTheSessionCap() {
        TerminalSession neverInitialisedSession = sessionWithoutEmulatorInitialisation("session-alpha");

        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        countedSessions.add(new SessionDefinitionCapCountPlanner.CountedSession(
            neverInitialisedSession.mSessionName, neverInitialisedSession.isRunning()));

        assertEquals("a session that never initialised an emulator owns no shell process and must not "
                + "occupy a slot in the session cap",
            0, new SessionDefinitionCapCountPlanner().countSessionsTowardCap(countedSessions));
    }

    @Test
    public void aReplacementSessionThatNeverReceivedAnEmulatorMustNotBeTreatedAsAHungAliveCandidate() {
        TerminalSession neverInitialisedSession = sessionWithoutEmulatorInitialisation("session-alpha");

        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession(
            neverInitialisedSession.mSessionName, neverInitialisedSession.isRunning(), false, true,
            1L));

        List<String> plannedSessionNames = new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, "ssh {name}");

        assertEquals("a replacement session that never received an emulator reports itself as running "
                + "and therefore lands in the permanently-hung reconnect branch",
            new ArrayList<String>(), plannedSessionNames);
    }
}
