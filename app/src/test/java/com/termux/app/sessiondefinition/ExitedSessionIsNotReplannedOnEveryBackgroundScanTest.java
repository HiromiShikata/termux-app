package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExitedSessionIsNotReplannedOnEveryBackgroundScanTest {

    private static final String AUTOSSH_COMMAND_TEMPLATE = "ssh {name}";

    private static final String SESSION_THAT_KEEPS_DYING = "https://example.test/keeps-dying-on-start";

    private static final String SESSION_THAT_JUST_DIED = "https://example.test/just-died";

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

    private static DeadSessionReconnectPlanner.CandidateSession deadSession(
            String sessionName, boolean readyToReconnectAfterExit) {
        return new DeadSessionReconnectPlanner.CandidateSession(sessionName, false, false, false,
            null, false, false, readyToReconnectAfterExit);
    }

    @Test
    public void aDeadSessionStillInsideItsWaitIsNotPlannedAgain() {
        List<String> plannedSessionNames = planner.planSessionNamesToReconnect(
            Collections.singletonList(deadSession(SESSION_THAT_KEEPS_DYING, false)),
            AUTOSSH_COMMAND_TEMPLATE);

        Assert.assertEquals("a session whose shell exits as soon as it starts is planned again on"
                + " every background scan, which is a loop with no exit condition, so while it is"
                + " inside the growing wait it must not be planned at all. Planned: "
                + plannedSessionNames,
            Collections.emptyList(), plannedSessionNames);
    }

    @Test
    public void aDeadSessionPastItsWaitIsPlanned() {
        List<String> plannedSessionNames = planner.planSessionNamesToReconnect(
            Collections.singletonList(deadSession(SESSION_THAT_JUST_DIED, true)),
            AUTOSSH_COMMAND_TEMPLATE);

        Assert.assertEquals("waiting must delay a reconnect, never abandon the session",
            Collections.singletonList(SESSION_THAT_JUST_DIED), plannedSessionNames);
    }

    @Test
    public void onlyTheSessionInsideItsWaitIsHeldBack() {
        List<String> plannedSessionNames = planner.planSessionNamesToReconnect(Arrays.asList(
            deadSession(SESSION_THAT_KEEPS_DYING, false),
            deadSession(SESSION_THAT_JUST_DIED, true)), AUTOSSH_COMMAND_TEMPLATE);

        Assert.assertEquals("holding one session back must not stop any other dead session being"
                + " reconnected in the same scan",
            Collections.singletonList(SESSION_THAT_JUST_DIED), plannedSessionNames);
    }
}
