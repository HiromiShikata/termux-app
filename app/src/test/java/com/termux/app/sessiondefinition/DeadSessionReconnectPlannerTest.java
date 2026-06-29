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
}
