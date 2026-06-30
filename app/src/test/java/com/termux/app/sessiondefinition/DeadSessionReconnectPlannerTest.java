package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeadSessionReconnectPlannerTest {

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

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
    public void reconnectsAtMostOneHungSessionPerTickChoosingOldestOut() {
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
            Collections.singletonList("https://example.test/hung-oldest"),
            namesToReconnect);
    }

    @Test
    public void reconnectsAllDeadButOnlyOneHungInSameTick() {
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
                "https://example.test/hung-oldest"),
            namesToReconnect);
    }
}
