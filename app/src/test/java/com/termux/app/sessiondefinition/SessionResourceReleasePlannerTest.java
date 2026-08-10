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
            hidden, false);
    }

    private static SessionResourceReleasePlanner.CandidateSession candidateUnderAClosedProject(
            String name, boolean running, boolean current) {
        return new SessionResourceReleasePlanner.CandidateSession(name, running, current, false,
            false, true);
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
    public void releasesAHiddenSessionEvenWhileItsRowIsOnScreen() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-hidden-on-screen", true, false, true, true)));

        Assert.assertEquals("a hidden session holds no runtime resources whatever the owner has on "
                + "screen, so a hidden row that is on screen with hidden rows shown is released like "
                + "any other",
            Collections.singletonList("session-hidden-on-screen"), released);
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
    public void releasesARunningSessionTheOwnerClosedTheProjectOf() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidateUnderAClosedProject("session-under-a-closed-project", true, false)));

        Assert.assertEquals("a session under a project the owner closed is out of sight exactly as a"
                + " hidden session is, and it holds a forked shell process that counts against the"
                + " number of processes Android lets the app hold, so closing a project has to free"
                + " that process and the cap slot it occupies",
            Collections.singletonList("session-under-a-closed-project"), released);
    }

    @Test
    public void keepsTheResourcesOfTheCurrentSessionEvenWhenItsProjectIsClosed() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidateUnderAClosedProject("session-current-under-a-closed-project", true, true)));

        Assert.assertEquals(new ArrayList<String>(), released);
    }

    @Test
    public void keepsARunningSessionWhileTheProjectLayoutIsNotKnownYet() {
        List<String> released = planner.planSessionNamesToRelease(Collections.singletonList(
            candidate("session-running-out-of-sight", true, false, false, false)));

        Assert.assertEquals("the set of sessions under open projects is empty before the session list"
                + " has been built, and releasing every running session on that empty set would kill"
                + " every shell the owner has, so a running session is released only when it is known"
                + " to sit under a project the owner closed",
            new ArrayList<String>(), released);
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
