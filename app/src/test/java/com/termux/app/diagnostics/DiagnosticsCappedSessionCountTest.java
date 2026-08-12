package com.termux.app.diagnostics;

import com.termux.app.sessiondefinition.SessionDefinitionCapCountPlanner;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Verifies that the diagnostics "Counted toward cap" figure is derived from the same live-only rule
 * the max-session cap enforces (see {@link SessionDefinitionCapCountPlanner}), so dead/orphan sessions
 * are excluded and the number matches what the cap actually uses rather than the total session count.
 */
public class DiagnosticsCappedSessionCountTest {

    private final SessionDefinitionCapCountPlanner planner = new SessionDefinitionCapCountPlanner();

    @Test
    public void countedTowardCapEqualsLiveCapCountAndExcludesDeadSessions() {
        int aliveCount = 39;
        int deadCount = 25;

        List<SessionDefinitionCapCountPlanner.CountedSession> snapshots = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            snapshots.add(new SessionDefinitionCapCountPlanner.CountedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadCount; i++) {
            // A mix of orphan (null name) and dead-but-reconnectable (named) dead sessions.
            String name = (i % 2 == 0) ? null : "autossh-dead-" + i;
            snapshots.add(new SessionDefinitionCapCountPlanner.CountedSession(name, false));
        }

        int countedTowardCap = planner.countSessionsTowardCap(snapshots, Collections.emptySet());

        // Must equal the live-only cap count, never the total (alive + dead) session count.
        Assert.assertEquals(aliveCount, countedTowardCap);
        Assert.assertNotEquals(aliveCount + deadCount, countedTowardCap);
    }

    @Test
    public void allDeadSessionsCountAsZeroTowardCap() {
        List<SessionDefinitionCapCountPlanner.CountedSession> snapshots = Arrays.asList(
            new SessionDefinitionCapCountPlanner.CountedSession(null, false),
            new SessionDefinitionCapCountPlanner.CountedSession("host-a", false),
            new SessionDefinitionCapCountPlanner.CountedSession("autossh-host-b", false));

        Assert.assertEquals(0, planner.countSessionsTowardCap(snapshots, Collections.emptySet()));
    }

    @Test
    public void orphanedCountIsRecomputedFromTheCorrectedLiveCappedCount() {
        int aliveCount = 10;
        int deadCount = 13;

        List<SessionDefinitionCapCountPlanner.CountedSession> snapshots = new ArrayList<>();
        for (int i = 0; i < aliveCount; i++) {
            snapshots.add(new SessionDefinitionCapCountPlanner.CountedSession("alive-" + i, true));
        }
        for (int i = 0; i < deadCount; i++) {
            snapshots.add(new SessionDefinitionCapCountPlanner.CountedSession(null, false));
        }

        int countedTowardCap = planner.countSessionsTowardCap(snapshots, Collections.emptySet());
        int displayedCount = 7; // fewer than live because some live sessions are hidden/collapsed

        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, 0L,
            countedTowardCap, displayedCount, 64, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsWorkCostLine(0, 0, 0, 0),
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", java.util.Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), ScrollbarViewCensus.empty(),
            0L, new DiagnosticsBackgroundCycle(0L, java.util.Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(), DiagnosticsShellExits.NONE, DiagnosticsPhantomProcessMonitor.UNMEASURED, DiagnosticsAppProcessPopulation.UNMEASURED);

        // Orphaned is derived from the corrected live count (10), not the total 23 sessions.
        Assert.assertEquals(aliveCount, report.getSessionsCountedTowardCap());
        Assert.assertEquals(aliveCount - displayedCount, report.getOrphanedSessionCount());
    }
}
