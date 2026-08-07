package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class DeadSessionReconnectPlannerDetachedSessionTest {

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

    @Test
    public void theSessionTheUserIsLookingAtIsReconnectedWhenItCannotReceiveInput() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/stuck", true, true, false, null, false, true));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/stuck"), namesToReconnect);
    }

    @Test
    public void aSessionThatIsAlreadyReconnectingIsLeftAlone() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/stuck", true, true, false, null, true, true));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void aSessionThatCanReceiveInputIsNotReconnectedJustBecauseItIsDisplayed() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/healthy", true, true, false, null, false, false));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertTrue(namesToReconnect.isEmpty());
    }

    @Test
    public void aDeadSessionThatAlsoCannotReceiveInputIsPlannedOnlyOnce() {
        List<DeadSessionReconnectPlanner.CandidateSession> candidates = Collections.singletonList(
            new DeadSessionReconnectPlanner.CandidateSession(
                "https://example.test/dead", false, false, false, null, false, true));

        List<String> namesToReconnect = planner.planSessionNamesToReconnect(candidates, "ssh {name}");

        Assert.assertEquals(Collections.singletonList("https://example.test/dead"), namesToReconnect);
    }
}
