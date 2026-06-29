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
    public void reconnectsRunningHungSession() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/hung", true, false, true));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/hung"), namesToReconnect);
    }

    @Test
    public void doesNotReconnectRunningHealthySession() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/healthy", true, false, false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void neverReconnectsCurrentSessionEvenWhenHung() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/current", true, true, true));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void neverReconnectsCurrentSessionEvenWhenFinished() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/current", false, true, false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void reconnectsHungAndFinishedNonCurrentSessions() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Arrays.asList(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/current", true, true, true),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/hung", true, false, true),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/healthy", true, false, false),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/dead", false, false, false));

        List<String> namesToReconnect =
            planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(
            Arrays.asList("https://example.test/hung", "https://example.test/dead"),
            namesToReconnect);
    }
}
