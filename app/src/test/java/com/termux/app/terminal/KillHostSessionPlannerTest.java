package com.termux.app.terminal;

import com.termux.app.terminal.session.TransientCommandSessionName;

import org.junit.Assert;
import org.junit.Test;

public class KillHostSessionPlannerTest {

    private static final String KILL_TEMPLATE = "ssh host tmux kill-session -t {name}";

    @Test
    public void anUnconfiguredTemplateIsReportedAsItsOwnCauseAndProducesNoCommand() {
        KillHostSessionPlan plan = KillHostSessionPlanner.plan(null, "host-session");

        Assert.assertEquals(KillHostSessionPlan.Outcome.COMMAND_NOT_CONFIGURED, plan.getOutcome());
        Assert.assertFalse(plan.shouldStart());
        Assert.assertNull("there is no fallback command for an unconfigured template", plan.getCommand());
        Assert.assertNull(plan.getSessionName());
    }

    @Test
    public void aBlankTemplateIsReportedAsUnconfiguredRatherThanAsAMissingSessionName() {
        Assert.assertEquals(KillHostSessionPlan.Outcome.COMMAND_NOT_CONFIGURED,
            KillHostSessionPlanner.plan("", "host-session").getOutcome());
        Assert.assertEquals(KillHostSessionPlan.Outcome.COMMAND_NOT_CONFIGURED,
            KillHostSessionPlanner.plan("   \n", "host-session").getOutcome());
    }

    @Test
    public void aMissingSessionNameIsReportedAsItsOwnCauseSoAConfiguredTemplateIsNeverCalledUnconfigured() {
        Assert.assertEquals(KillHostSessionPlan.Outcome.SESSION_NAME_MISSING,
            KillHostSessionPlanner.plan(KILL_TEMPLATE, null).getOutcome());
        Assert.assertEquals(KillHostSessionPlan.Outcome.SESSION_NAME_MISSING,
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "").getOutcome());
    }

    @Test
    public void aStartPlanCarriesTheComposedCommandForTheNormalizedHostSessionName() {
        KillHostSessionPlan plan = KillHostSessionPlanner.plan(KILL_TEMPLATE, "github.com:8080");

        Assert.assertTrue(plan.shouldStart());
        Assert.assertEquals("ssh host tmux kill-session -t 'github_com_8080'", plan.getCommand());
    }

    @Test
    public void aStartPlanNamesTheCommandSessionSoItIsRecognizedAsTransient() {
        KillHostSessionPlan plan = KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session");

        Assert.assertEquals(TransientCommandSessionName.forKillOfSession("host-session"),
            plan.getSessionName());
        Assert.assertTrue(TransientCommandSessionName.isTransient(plan.getSessionName()));
    }

    @Test
    public void theCommandSessionNameIsDerivedFromTheHostSessionSoRepeatedKillsCollideByName() {
        Assert.assertEquals(KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName(),
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName());
        Assert.assertNotEquals(KillHostSessionPlanner.plan(KILL_TEMPLATE, "host-session").getSessionName(),
            KillHostSessionPlanner.plan(KILL_TEMPLATE, "another-session").getSessionName());
    }
}
