package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;
import com.termux.app.terminal.session.FinishedSessionEnterAction;
import com.termux.app.terminal.session.TransientCommandSessionName;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HostKillCommandSessionLifecycleTest {

    private static final String CONNECT_TEMPLATE = "autossh -M 0 {name}";

    private static final String KILL_TEMPLATE = "ssh host tmux kill-session -t {name}";

    @Test
    public void aSecondKillForTheSameHostSessionPlansTheSameCommandSessionName() {
        Assert.assertEquals("repeated taps must resolve to one command session, not to a growing pile of "
                + "identically-targeted sessions",
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName(),
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName());
    }

    @Test
    public void aKillForADifferentHostSessionPlansItsOwnCommandSessionName() {
        Assert.assertNotEquals(
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName(),
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "another-host-session").getSessionName());
    }

    @Test
    public void anExitedKillCommandSessionIsNeverPlannedForBackgroundReconnect() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession(
            TransientCommandSessionName.forKillOfSession("host-session"), false));

        List<String> sessionNamesToReconnect = new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, CONNECT_TEMPLATE);

        Assert.assertTrue("a short-lived command session must never be reconnected as if it were a host "
                + "session, otherwise a phantom connect session is created for a host session that does not exist",
            sessionNamesToReconnect.isEmpty());
    }

    @Test
    public void anOrdinaryHostSessionIsStillPlannedForBackgroundReconnect() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession("host-session", false));

        Assert.assertEquals(List.of("host-session"), new DeadSessionReconnectPlanner()
            .planSessionNamesToReconnect(candidateSessions, CONNECT_TEMPLATE));
    }

    @Test
    public void anExitedKillCommandSessionIsRemovedRatherThanReconnected() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            TransientCommandSessionName.forKillOfSession("host-session"), CONNECT_TEMPLATE);

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
    }
}
