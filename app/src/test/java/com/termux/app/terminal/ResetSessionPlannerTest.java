package com.termux.app.terminal;

import com.termux.app.terminal.session.TransientCommandSessionName;

import org.junit.Assert;
import org.junit.Test;

public class ResetSessionPlannerTest {

    @Test
    public void placeholderIsReplacedWithQuotedSessionName() {
        Assert.assertEquals("ssh host /opt/reset.sh 'work-session'",
            ResetSessionPlanner.plan("ssh host /opt/reset.sh {name}", "work-session").getCommand());
    }

    @Test
    public void dotsInSessionNameAreNormalizedToUnderscoresBeforeQuoting() {
        Assert.assertEquals("ssh host /opt/reset.sh 'host_example_com'",
            ResetSessionPlanner.plan("ssh host /opt/reset.sh {name}", "host.example.com").getCommand());
    }

    @Test
    public void colonsInSessionNameAreNormalizedToUnderscoresBeforeQuoting() {
        Assert.assertEquals("ssh host /opt/reset.sh 'host_8080'",
            ResetSessionPlanner.plan("ssh host /opt/reset.sh {name}", "host:8080").getCommand());
    }

    @Test
    public void dotsAndColonsTogetherAreNormalizedBeforeQuoting() {
        Assert.assertEquals("ssh host /opt/reset.sh 'host_example_com_8080'",
            ResetSessionPlanner.plan("ssh host /opt/reset.sh {name}", "host.example.com:8080").getCommand());
    }

    @Test
    public void singleQuoteInSessionNameIsEscapedForPosixShells() {
        Assert.assertEquals("ssh host /opt/reset.sh 'it'\\''s'",
            ResetSessionPlanner.plan("ssh host /opt/reset.sh {name}", "it's").getCommand());
    }

    @Test
    public void everyPlaceholderOccurrenceIsReplaced() {
        Assert.assertEquals("echo 'a_b' && reset 'a_b'",
            ResetSessionPlanner.plan("echo {name} && reset {name}", "a.b").getCommand());
    }

    @Test
    public void plannedSessionRunsUnderATransientNameDistinctFromTheSessionBeingReset() {
        ResetSessionPlan plan = ResetSessionPlanner.plan("reset.sh {name}", "work-session");

        Assert.assertEquals("[reset] work-session", plan.getSessionName());
        Assert.assertNotEquals("work-session", plan.getSessionName());
        Assert.assertTrue(TransientCommandSessionName.isTransient(plan.getSessionName()));
    }

    @Test
    public void emptyTemplateReportsCommandNotConfigured() {
        ResetSessionPlan plan = ResetSessionPlanner.plan("", "work-session");

        Assert.assertEquals(ResetSessionPlan.Outcome.COMMAND_NOT_CONFIGURED, plan.getOutcome());
        Assert.assertFalse(plan.shouldStart());
        Assert.assertNull(plan.getCommand());
    }

    @Test
    public void blankTemplateReportsCommandNotConfigured() {
        Assert.assertEquals(ResetSessionPlan.Outcome.COMMAND_NOT_CONFIGURED,
            ResetSessionPlanner.plan("   \n  ", "work-session").getOutcome());
    }

    @Test
    public void nullTemplateReportsCommandNotConfigured() {
        Assert.assertEquals(ResetSessionPlan.Outcome.COMMAND_NOT_CONFIGURED,
            ResetSessionPlanner.plan(null, "work-session").getOutcome());
    }

    @Test
    public void configuredTemplateWithUnnamedSessionIsNotReportedAsNotConfigured() {
        ResetSessionPlan nullNamePlan = ResetSessionPlanner.plan("reset.sh {name}", null);
        ResetSessionPlan emptyNamePlan = ResetSessionPlanner.plan("reset.sh {name}", "");

        Assert.assertEquals(ResetSessionPlan.Outcome.SESSION_NAME_MISSING, nullNamePlan.getOutcome());
        Assert.assertEquals(ResetSessionPlan.Outcome.SESSION_NAME_MISSING, emptyNamePlan.getOutcome());
        Assert.assertFalse(nullNamePlan.shouldStart());
        Assert.assertFalse(emptyNamePlan.shouldStart());
    }
}
