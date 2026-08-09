package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The planner is where a reconnect stops being a rule and becomes a session name, so it is the only
 * place that still knows which of its three rules planned each one. A plan that carries names alone
 * throws that away, and no later stage can recover it.
 */
public class DeadSessionReconnectPlannerReasonTest {

    private static final String AUTOSSH_COMMAND_TEMPLATE = "ssh {name}";

    private final DeadSessionReconnectPlanner planner = new DeadSessionReconnectPlanner();

    private List<PlannedSessionReconnect> plannedReconnectsFor(
        DeadSessionReconnectPlanner.CandidateSession... candidateSessions) {
        return planner.planReconnects(Arrays.asList(candidateSessions), AUTOSSH_COMMAND_TEMPLATE,
            DeadSessionReconnectPlanner.UNLIMITED, Collections.<String>emptySet());
    }

    private static SessionReconnectReason onlyReasonOf(List<PlannedSessionReconnect> plannedReconnects) {
        Assert.assertEquals("exactly one session was expected to be planned", 1, plannedReconnects.size());
        return plannedReconnects.get(0).getReason();
    }

    @Test
    public void aSessionWhoseShellProcessIsGoneIsAttributedToTheShellProcessRule() {
        List<PlannedSessionReconnect> plannedReconnects = plannedReconnectsFor(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/exited", false));

        Assert.assertEquals(SessionReconnectReason.SHELL_PROCESS_GONE_AT_THE_BACKGROUND_SCAN,
            onlyReasonOf(plannedReconnects));
    }

    @Test
    public void aRunningSessionWhoseInputNoLongerArrivesIsAttributedToTheInputRule() {
        List<PlannedSessionReconnect> plannedReconnects = plannedReconnectsFor(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/detached", true,
                false, false, 1783216800000L, false, true));

        Assert.assertEquals(SessionReconnectReason.INPUT_NO_LONGER_REACHES_THE_PROGRAM,
            onlyReasonOf(plannedReconnects));
    }

    @Test
    public void aRunningSessionThatIsMerelyQuietIsAttributedToTheStalenessRule() {
        List<PlannedSessionReconnect> plannedReconnects = plannedReconnectsFor(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/quiet", true,
                false, true, 1783216800000L));

        Assert.assertEquals(SessionReconnectReason.SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD,
            onlyReasonOf(plannedReconnects));
    }

    @Test
    public void eachPlannedSessionKeepsItsOwnReasonWhenSeveralRulesFireInTheSamePass() {
        List<PlannedSessionReconnect> plannedReconnects = plannedReconnectsFor(
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/quiet", true,
                false, true, 1783216800000L),
            new DeadSessionReconnectPlanner.CandidateSession("https://example.test/exited", false));

        Assert.assertEquals(2, plannedReconnects.size());
        for (PlannedSessionReconnect plannedReconnect : plannedReconnects) {
            SessionReconnectReason expectedReason =
                plannedReconnect.getSessionName().endsWith("/exited")
                    ? SessionReconnectReason.SHELL_PROCESS_GONE_AT_THE_BACKGROUND_SCAN
                    : SessionReconnectReason.SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD;
            Assert.assertEquals("a pass that mixes rules must not collapse them onto one reason",
                expectedReason, plannedReconnect.getReason());
        }
    }
}
