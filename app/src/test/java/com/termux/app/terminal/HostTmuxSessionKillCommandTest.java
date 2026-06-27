package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxSessionKillCommandTest {

    @Test
    public void sendsTmuxPrefixThenCommandPromptKillForSelectedSessionName() {
        Assert.assertEquals(":kill-session -t 'host-session'\n",
            HostTmuxSessionKillCommand.forSessionName("host-session"));
    }

    @Test
    public void prefixesWithControlBByDefaultSoTmuxInterceptsRegardlessOfForegroundProgram() {
        String command = HostTmuxSessionKillCommand.forSessionName("host-session");
        Assert.assertEquals(0x02, command.charAt(0));
        Assert.assertTrue(command.startsWith(":"));
    }

    @Test
    public void usesProvidedPrefixKeyWhenHostCustomizedTheTmuxPrefix() {
        Assert.assertEquals(":kill-session -t 'host-session'\n",
            HostTmuxSessionKillCommand.forSessionName("host-session", (char) 0x01));
    }

    @Test
    public void shellQuotesSessionNameWithSingleQuote() {
        Assert.assertEquals(":kill-session -t 'it'\\''s'\n",
            HostTmuxSessionKillCommand.forSessionName("it's"));
    }

    @Test
    public void quotesSessionNameContainingShellMetacharacters() {
        Assert.assertEquals(":kill-session -t 'a;b c$d'\n",
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
