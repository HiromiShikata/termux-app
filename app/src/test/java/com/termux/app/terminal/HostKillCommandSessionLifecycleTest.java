package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;
import com.termux.app.terminal.session.FinishedSessionEnterAction;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HostKillCommandSessionLifecycleTest {

    private static final String CONNECT_TEMPLATE = "autossh -M 0 {name}";

    @Test
    public void aSecondKillForTheSameHostSessionIsRefusedWhileTheFirstCommandIsStillRunning() {
        HostKillCommandSessionRegistry registry = new HostKillCommandSessionRegistry();

        registry.record("command-session-handle", "host-session");

        Assert.assertTrue("repeated taps must not start a second command session for the same host session",
            registry.isDispatchedFor("host-session"));
    }

    @Test
    public void aKillForADifferentHostSessionIsStillAllowedWhileOneCommandIsRunning() {
        HostKillCommandSessionRegistry registry = new HostKillCommandSessionRegistry();

        registry.record("command-session-handle", "host-session");

        Assert.assertFalse(registry.isDispatchedFor("another-host-session"));
    }

    @Test
    public void theCommandSessionIsTakenOutOfTheSessionListOnceItsCommandHasExited() {
        HostKillCommandSessionRegistry registry = new HostKillCommandSessionRegistry();
        registry.record("command-session-handle", "host-session");

        Assert.assertTrue("the finished command session must be removed instead of lingering in the list",
            registry.forgetFinishedCommandSession("command-session-handle"));
    }

    @Test
    public void aFinishedSessionThatIsNotAKillCommandSessionIsLeftInTheListUntouched() {
        HostKillCommandSessionRegistry registry = new HostKillCommandSessionRegistry();
        registry.record("command-session-handle", "host-session");

        Assert.assertFalse(registry.forgetFinishedCommandSession("some-other-session-handle"));
        Assert.assertFalse(registry.forgetFinishedCommandSession(null));
    }

    @Test
    public void anotherKillForTheSameHostSessionIsAllowedAfterTheEarlierCommandExited() {
        HostKillCommandSessionRegistry registry = new HostKillCommandSessionRegistry();
        registry.record("command-session-handle", "host-session");

        registry.forgetFinishedCommandSession("command-session-handle");

        Assert.assertFalse("the guard must not block kills forever once the command session has exited",
            registry.isDispatchedFor("host-session"));
    }

    @Test
    public void anExitedKillCommandSessionIsNeverPlannedForBackgroundReconnect() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession(null, false));

        List<String> sessionNamesToReconnect = new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, CONNECT_TEMPLATE);

        Assert.assertTrue("an unnamed short-lived command session must never be reconnected as if it were a host "
                + "session, otherwise a phantom connect session is created for a host session that does not exist",
            sessionNamesToReconnect.isEmpty());
    }

    @Test
    public void anExitedKillCommandSessionIsRemovedRatherThanReconnected() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(null, CONNECT_TEMPLATE);

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
    }
}
