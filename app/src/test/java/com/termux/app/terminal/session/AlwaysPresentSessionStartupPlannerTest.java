package com.termux.app.terminal.session;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class AlwaysPresentSessionStartupPlannerTest {

    private static final String SHELL_PATH = "/data/data/com.termux/files/usr/bin/sh";

    private final AlwaysPresentSessionStartupPlanner planner = new AlwaysPresentSessionStartupPlanner();

    @Test
    public void planStartupBuildsAutosshCommandWhenTemplateConfigured() {
        AlwaysPresentSessionStartup startup = planner.planStartup("myhost", "ssh {name}", SHELL_PATH);

        Assert.assertEquals("myhost", startup.getName());
        Assert.assertTrue(startup.hasCommand());
        Assert.assertEquals(SHELL_PATH, startup.getExecutablePath());
        Assert.assertArrayEquals(new String[]{"-c", "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'myhost'"}, startup.getArguments());
    }

    @Test
    public void planStartupProducesPlainSessionWhenTemplateBlank() {
        AlwaysPresentSessionStartup startup = planner.planStartup("myhost", "   ", SHELL_PATH);

        Assert.assertEquals("myhost", startup.getName());
        Assert.assertFalse(startup.hasCommand());
        Assert.assertNull(startup.getExecutablePath());
        Assert.assertNull(startup.getArguments());
    }

    @Test
    public void planStartupProducesPlainSessionWhenTemplateNull() {
        AlwaysPresentSessionStartup startup = planner.planStartup("myhost", null, SHELL_PATH);

        Assert.assertFalse(startup.hasCommand());
        Assert.assertNull(startup.getExecutablePath());
        Assert.assertNull(startup.getArguments());
    }

    @Test
    public void planStartupShellQuotesSessionNameWithSingleQuote() {
        AlwaysPresentSessionStartup startup = planner.planStartup("a'b", "ssh {name}", SHELL_PATH);

        Assert.assertArrayEquals(new String[]{"-c", "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'a'\\''b'"}, startup.getArguments());
    }

    @Test
    public void planStartupBuildsDistinctCommandForEachConfiguredName() {
        AlwaysPresentSessionStartup alpha = planner.planStartup("alpha", "ssh {name}", SHELL_PATH);
        AlwaysPresentSessionStartup beta = planner.planStartup("beta", "ssh {name}", SHELL_PATH);

        Assert.assertEquals(Arrays.asList("-c", "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'alpha'"), Arrays.asList(alpha.getArguments()));
        Assert.assertEquals(Arrays.asList("-c", "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'beta'"), Arrays.asList(beta.getArguments()));
    }

    @Test
    public void planStartupTrimsTemplateBeforeSubstitution() {
        AlwaysPresentSessionStartup startup = planner.planStartup("myhost", "  ssh {name}  ", SHELL_PATH);

        Assert.assertArrayEquals(new String[]{"-c", "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'myhost'"}, startup.getArguments());
    }

    @Test
    public void plannedStartupReturnsDefensiveArgumentsCopy() {
        AlwaysPresentSessionStartup startup = planner.planStartup("myhost", "ssh {name}", SHELL_PATH);

        startup.getArguments()[1] = "tampered";

        Assert.assertArrayEquals(new String[]{"-c", "ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'myhost'"}, startup.getArguments());
    }
}
