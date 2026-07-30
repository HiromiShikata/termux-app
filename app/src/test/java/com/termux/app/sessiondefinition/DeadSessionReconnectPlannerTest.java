package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeadSessionReconnectPlannerTest {

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

    @Test
    public void doesNotReconnectDeadSessionThatUserExplicitlyRemoved() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("google logon", false),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead", false));
        Set<String> userRemovedSessionNames = new HashSet<>();
        userRemovedSessionNames.add("google logon");

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(
            candidates, "ssh {name}", DeadSessionReconnectPlanner.UNLIMITED, userRemovedSessionNames);

        Assert.assertEquals(Collections.singletonList("https://example.test/dead"), namesToReconnect);
    }

    @Test
    public void reconnectsOnlyDeadDefinitionBackedSessions() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/live", true),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead", false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/dead"), namesToReconnect);
    }

    @Test
    public void neverReconnectsLiveSessions() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/a", true),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/b", true));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void doesNotReconnectDeadSessionWhenNoAutosshTemplateConfigured() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead", false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void doesNotReconnectDeadSessionWithBlankName() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession("   ", false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void preservesOrderOfDeadSessions() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead1", false),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/live", true),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead2", false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(
            Arrays.asList("https://example.test/dead1", "https://example.test/dead2"),
            namesToReconnect);
    }

    @Test
    public void reconnectsDeadNonCurrentSession() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/dead", false, false, false, null));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/dead"), namesToReconnect);
    }

    @Test
    public void reconnectsHungNonCurrentSession() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung", true, false, true, 1L));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/hung"), namesToReconnect);
    }

    @Test
    public void neverReconnectsCurrentSessionWhenDead() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/current", false, true, false, null));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void neverReconnectsCurrentSessionWhenHung() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/current", true, true, true, 1L));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void doesNotReconnectHealthyNonCurrentSession() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/healthy", true, false, false, 1L));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void reconnectsAllHungSessionsOldestOutFirst() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-newer", true, false, true, 5_000L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-oldest", true, false, true, 1_000L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-middle", true, false, true, 3_000L));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(
            Arrays.asList(
                "https://example.test/hung-oldest",
                "https://example.test/hung-middle",
                "https://example.test/hung-newer"),
            namesToReconnect);
    }

    @Test
    public void capOfOneReconnectsOnlyASingleDeadSession() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead1", false),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead2", false),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead3", false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}", 1);

        Assert.assertEquals(Collections.singletonList("https://example.test/dead1"), namesToReconnect);
    }

    @Test
    public void capPrefersDeadSessionsOverHungSessionsWhenOnlyOneSlotIsAvailable() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung", true, false, true, 1_000L),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead", false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}", 1);

        Assert.assertEquals(Collections.singletonList("https://example.test/dead"), namesToReconnect);
    }

    @Test
    public void capOfOneReconnectsTheOldestHungSessionWhenNoDeadSessionsExist() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-newer", true, false, true, 5_000L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-oldest", true, false, true, 1_000L));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}", 1);

        Assert.assertEquals(Collections.singletonList("https://example.test/hung-oldest"), namesToReconnect);
    }

    @Test
    public void aNonPositiveCapReconnectsNothing() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead1", false),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead2", false));

        Assert.assertTrue(planner.planSessionNamesToReconnect(candidates, "ssh {name}", 0).isEmpty());
        Assert.assertTrue(planner.planSessionNamesToReconnect(candidates, "ssh {name}", -1).isEmpty());
    }

    @Test
    public void reconnectsAllDeadAndAllHungInSameTickExcludingCurrentAndHealthy() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/current", true, true, true, 1L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-newer", true, false, true, 4_000L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/hung-oldest", true, false, true, 2_000L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/healthy", true, false, false, 9_000L),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/dead1", false, false, false, null),
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/dead2", false, false, false, null));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(
            Arrays.asList(
                "https://example.test/dead1",
                "https://example.test/dead2",
                "https://example.test/hung-oldest",
                "https://example.test/hung-newer"),
            namesToReconnect);
    }

    @Test
    public void doesNotReselectASessionWhoseReconnectIsStillInFlight() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/reconnecting", false, false, false, null, true));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.emptyList(), namesToReconnect);
    }

    @Test
    public void doesNotReselectAHungSessionWhoseReconnectIsStillInFlight() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/reconnecting", true, false, true, 1L, true));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.emptyList(), namesToReconnect);
    }

    @Test
    public void reselectsASessionOnceItsReconnectHasSettled() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/settled", false, false, false, null, false));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/settled"), namesToReconnect);
    }

    @Test
    public void aCandidateThatIsNotRunningIsNeverCarriedAsHung() {
        DeadSessionReconnectPlanner.CandidateSession candidate =
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/dead", false, false, true, 1L, false);

        Assert.assertFalse("hung means alive but no longer producing output, so a candidate whose "
                + "shell process has exited must not carry the hung flag whatever its caller passes, "
                + "otherwise a state the running system cannot produce becomes constructible here",
            candidate.isHung());
    }
}
