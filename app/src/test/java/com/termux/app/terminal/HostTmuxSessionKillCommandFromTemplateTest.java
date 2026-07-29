package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxSessionKillCommandFromTemplateTest {

    @Test
    public void substitutesNormalizedSessionNameIntoConfiguredTemplate() {
        Assert.assertEquals("tmux kill-session -t 'myhost_example_2'",
            HostTmuxSessionKillCommand.forSessionName("myhost.example:2", "tmux kill-session -t {name}"));
    }

    @Test
    public void shellQuotesSessionNameContainingSingleQuoteInsideTemplate() {
        String singleQuote = "'";
        String escapedQuoteSequence = "'\\''";
        String expectedQuotedName = singleQuote + "o" + escapedQuoteSequence + "brien" + singleQuote;

        Assert.assertEquals("kill " + expectedQuotedName + " now",
            HostTmuxSessionKillCommand.forSessionName("o'brien", "kill {name} now"));
    }

    @Test
    public void substitutesEveryOccurrenceOfNamePlaceholderInTemplate() {
        Assert.assertEquals("echo 'host' && tmux kill-session -t 'host'",
            HostTmuxSessionKillCommand.forSessionName("host", "echo {name} && tmux kill-session -t {name}"));
    }

    @Test
    public void returnsNullWhenCommandTemplateIsEmptySoCallerCanSurfaceNotConfiguredState() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("host-session", ""));
    }

    @Test
    public void returnsNullWhenCommandTemplateIsNullSoCallerCanSurfaceNotConfiguredState() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("host-session", null));
    }

    @Test
    public void returnsNullWhenSessionNameIsEmptyRegardlessOfConfiguredTemplate() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("", "tmux kill-session -t {name}"));
    }
}
