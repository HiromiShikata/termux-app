package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BackgroundReconnectGentlenessTest {

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

    @Test
    public void theBackgroundReconnectPlansEveryStaleSessionSoNoInfoStaysStaleBeyondATick() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = new ArrayList<>();
        for (int sessionNumber = 0; sessionNumber < 20; sessionNumber++) {
            candidates.add(new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/session" + sessionNumber, false));
        }

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(
            candidates, "ssh {name}", DeadSessionReconnectPlanner.UNLIMITED);

        Assert.assertEquals(20, namesToReconnect.size());
    }

    @Test
    public void theStaggerIntervalIsAtLeastOneSecondSoABacklogDoesNotSpikeFileDescriptors() {
        Assert.assertTrue(TermuxTerminalSessionActivityClient.STAGGERED_RECONNECT_INTERVAL_MILLIS >= 1000L);
    }

    @Test
    public void theBackgroundStalenessThresholdIsFresherThanTheHungThresholdSoInfoNeverReaches30MinStale() {
        Assert.assertTrue(
            TermuxTerminalSessionActivityClient.BACKGROUND_RECONNECT_STALE_OUT_MAX_AGE_MILLIS
                < HungSessionDetector.STALE_OUT_MAX_AGE_MILLIS);
    }
}
