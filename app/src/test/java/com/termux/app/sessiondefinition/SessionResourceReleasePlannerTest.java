package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionResourceReleasePlannerTest {

    private final SessionResourceReleasePlanner planner = new SessionResourceReleasePlanner();

    private static SessionResourceReleasePlanner.CandidateSession candidate(String name,
                                                                           boolean running,
                                                                           boolean current,
                                                                           boolean displayed,
                                                                           boolean hidden) {
        return new SessionResourceReleasePlanner.CandidateSession(name, running, current, displayed,
            hidden);
    }

    @Test
    public void releasesAHiddenSessionWhoseProcessIsStillAlive() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-hidden", true, false, false, true)));

        Assert.assertEquals(Collections.singletonList("session-hidden"), released);
    }

    @Test
    public void releasesAHiddenSessionWhoseProcessHasExited() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-hidden", false, false, false, true)));

        Assert.assertEquals(Collections.singletonList("session-hidden"), released);
    }

    @Test
    public void keepsTheResourcesOfAHiddenSessionWhoseRowTheOwnerIsLookingAt() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-hidden-on-screen", true, false, true, true)));

        Assert.assertEquals("a hidden row the owner has on screen still renders its call, out and reply "
                + "times from its own transcript, so releasing its emulator blanks a row the owner is "
                + "looking at",
            new ArrayList<String>(), released);
    }

    @Test
    public void releasesADeadSessionThatIsNeitherCurrentNorDisplayed() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-dead", false, false, false, false)));

        Assert.assertEquals(Collections.singletonList("session-dead"), released);
    }

    @Test
    public void keepsTheResourcesOfEveryDisplayedSession() {
        List<String> released = planner.planSessionNamesToRelease(Arrays.asList(
            candidate("session-displayed-alive", true, false, true, false),
            candidate("session-displayed-dead", false, false, true, false)));

        Assert.assertEquals(new ArrayList<String>(), released);
    }

    @Test
    public void keepsTheResourcesOfTheCurrentSessionEvenWhenItIsMarkedHidden() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-current", true, true, true, true)));

        Assert.assertEquals(new ArrayList<String>(), released);
    }

    @Test
    public void keepsTheResourcesOfAnUndisplayedSessionWhoseProcessIsStillAlive() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-background", true, false, false, false)));

        Assert.assertEquals(new ArrayList<String>(), released);
    }

    @Test
    public void ignoresUnnamedAndNullCandidates() {
        List<SessionResourceReleasePlanner.CandidateSession> candidateSessions = new ArrayList<>();
        candidateSessions.add(null);
        candidateSessions.add(candidate(null, false, false, false, true));

        Assert.assertEquals(new ArrayList<String>(),
            planner.planSessionNamesToRelease(candidateSessions));
    }
}
