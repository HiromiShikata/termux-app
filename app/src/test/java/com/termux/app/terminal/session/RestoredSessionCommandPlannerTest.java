package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class RestoredSessionCommandPlannerTest {

    private static final String SESSION_NAME = "https://example.test/session-1";
    private static final String PREVIOUS_TEMPLATE = "autossh -M 0 -t user@host \"start {name}\"";
    private static final String CONFIGURED_TEMPLATE = "ssh -t user@host \"start {name}\"";

    private final RestoredSessionCommandPlanner planner = new RestoredSessionCommandPlanner();

    private static String[] shellArgumentsFor(String template, String sessionName) {
        return new String[]{"-c", template.replace("{name}", "'" + sessionName + "'")};
    }

    @Test
    public void aRestoredSessionRunsTheConfiguredCommandRatherThanTheOneStoredWhenItWasCreated() {
        String[] planned = planner.planArguments(SESSION_NAME,
            shellArgumentsFor(PREVIOUS_TEMPLATE, SESSION_NAME), CONFIGURED_TEMPLATE);

        Assert.assertEquals("a shell command invocation is the flag and the command, so anything else"
                + " would not be a command the session can run", 2, planned.length);
        Assert.assertEquals("-c", planned[0]);
        Assert.assertTrue("the configured command decides how many operating-system processes the"
                + " session holds, so a restored session has to run it. Planned command: " + planned[1],
            planned[1].startsWith("ssh ") && planned[1].contains("-t user@host"));
        Assert.assertFalse("the stored argument vector is the command the session was created with,"
                + " and replaying it means the command the owner replaced survives every restart."
                + " Planned command: " + planned[1], planned[1].contains("autossh"));
    }

    @Test
    public void theSessionNameIsSubstitutedIntoTheConfiguredCommand() {
        String[] planned = planner.planArguments(SESSION_NAME,
            shellArgumentsFor(PREVIOUS_TEMPLATE, SESSION_NAME), CONFIGURED_TEMPLATE);

        Assert.assertTrue("a restored session must reach the same remote session it had before, so"
                + " its own name has to be substituted into the configured command. Planned command: "
                + planned[1], planned[1].contains("'" + SESSION_NAME + "'"));
    }

    @Test
    public void theConfiguredCommandIsPlannedWithTheKeepaliveOptionsEveryNewSessionGets() {
        String[] planned = planner.planArguments(SESSION_NAME,
            shellArgumentsFor(PREVIOUS_TEMPLATE, SESSION_NAME), CONFIGURED_TEMPLATE);

        Assert.assertTrue("a restored session has to be built the same way a newly created one is,"
                + " otherwise it would sit on a connection with no keepalive and stall unnoticed."
                + " Planned command: " + planned[1],
            planned[1].contains("-o ServerAliveInterval=30")
                && planned[1].contains("-o ConnectTimeout=10"));
    }

    @Test
    public void aSessionWithNoConfiguredCommandKeepsWhatItWasCreatedWith() {
        String[] stored = shellArgumentsFor(PREVIOUS_TEMPLATE, SESSION_NAME);

        Assert.assertSame("with no command configured there is nothing to rebuild from, and dropping"
                + " the stored argument vector would leave the session with no command at all",
            stored, planner.planArguments(SESSION_NAME, stored, "   "));
        Assert.assertSame(stored, planner.planArguments(SESSION_NAME, stored, null));
    }

    @Test
    public void aLocalShellSessionKeepsWhatItWasCreatedWith() {
        Assert.assertNull("a session created without a command is a plain local shell, and giving it"
                + " the configured remote command would change what the owner opened",
            planner.planArguments(SESSION_NAME, null, CONFIGURED_TEMPLATE));

        String[] loginShell = new String[]{"-l"};
        Assert.assertSame("an argument vector that is not the shell command flag and one command is"
                + " not a session built from the configured command", loginShell,
            planner.planArguments(SESSION_NAME, loginShell, CONFIGURED_TEMPLATE));
    }

    @Test
    public void anUnnamedSessionKeepsWhatItWasCreatedWith() {
        String[] stored = shellArgumentsFor(PREVIOUS_TEMPLATE, SESSION_NAME);

        Assert.assertSame("there is no name to substitute into the configured command, so the stored"
                + " argument vector is the only command this session has",
            stored, planner.planArguments(null, stored, CONFIGURED_TEMPLATE));
    }

    @Test
    public void planningIsStableForASessionAlreadyRunningTheConfiguredCommand() {
        String[] plannedOnce = planner.planArguments(SESSION_NAME,
            shellArgumentsFor(PREVIOUS_TEMPLATE, SESSION_NAME), CONFIGURED_TEMPLATE);
        String[] plannedTwice = planner.planArguments(SESSION_NAME, plannedOnce, CONFIGURED_TEMPLATE);

        Assert.assertEquals("a session is restored on every app start, so planning has to settle on"
                + " one command rather than growing options on each pass",
            plannedOnce[1], plannedTwice[1]);
    }
}
