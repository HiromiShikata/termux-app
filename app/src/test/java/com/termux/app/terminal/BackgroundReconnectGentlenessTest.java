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
    public void theStaggerSpacesReconnectsAboutOneSecondApartSoTheBatchNeverFiresSimultaneously() {
        StaggeredReconnectSchedule schedule = new StaggeredReconnectSchedule(
            TermuxTerminalSessionActivityClient.STAGGERED_RECONNECT_INTERVAL_MILLIS,
            TermuxTerminalSessionActivityClient.STAGGERED_RECONNECT_CONCURRENT_WINDOW);

        int concurrentWindow = TermuxTerminalSessionActivityClient.STAGGERED_RECONNECT_CONCURRENT_WINDOW;
        long intervalMillis = TermuxTerminalSessionActivityClient.STAGGERED_RECONNECT_INTERVAL_MILLIS;

        for (int reconnectIndex = 0; reconnectIndex < concurrentWindow; reconnectIndex++) {
            Assert.assertEquals(0L, schedule.startDelayMillisForIndex(reconnectIndex));
        }
        Assert.assertEquals(intervalMillis, schedule.startDelayMillisForIndex(concurrentWindow));
        Assert.assertEquals(intervalMillis, schedule.startDelayMillisForIndex(2 * concurrentWindow - 1));
        Assert.assertEquals(2 * intervalMillis, schedule.startDelayMillisForIndex(2 * concurrentWindow));
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
