package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BackgroundReconnectGentlenessTest {

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

    @Test
    public void theBackgroundReconnectCapIsGentleSoTheTickNeverFiresASimultaneousBatch() {
        Assert.assertEquals(1, TermuxTerminalSessionActivityClient.MAX_BACKGROUND_RECONNECTS_PER_TICK);
    }

    @Test
    public void withTwentyStaleSessionsTheBackgroundTickPlansAtMostOneReconnectInsteadOfAllTwenty() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = new ArrayList<>();
        for (int sessionNumber = 0; sessionNumber < 20; sessionNumber++) {
            candidates.add(new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/session" + sessionNumber, false));
        }

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(
            candidates, "ssh {name}",
            TermuxTerminalSessionActivityClient.MAX_BACKGROUND_RECONNECTS_PER_TICK);

        Assert.assertEquals(1, namesToReconnect.size());
    }
}
