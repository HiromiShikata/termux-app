package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxSessionKillCommandTest {

    @Test
    public void issuesHostSideTmuxKillForSelectedSessionName() {
        Assert.assertEquals("tmux kill-session -t 'umino'\n",
            HostTmuxSessionKillCommand.forSessionName("umino"));
    }

    @Test
    public void shellQuotesSessionNameWithSingleQuote() {
        Assert.assertEquals("tmux kill-session -t 'it'\\''s'\n",
            HostTmuxSessionKillCommand.forSessionName("it's"));
    }

    @Test
    public void quotesSessionNameContainingShellMetacharacters() {
        Assert.assertEquals("tmux kill-session -t 'a;b c$d'\n",
            HostTmuxSessionKillCommand.forSessionName("a;b c$d"));
    }

    @Test
    public void returnsNullForNullSessionName() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName(null));
    }

    @Test
    public void returnsNullForEmptySessionName() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName(""));
    }
}
