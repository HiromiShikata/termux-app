package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxCallToUserScrollCommandTest {

    private static final char PREFIX = HostTmuxCallToUserScrollCommand.DEFAULT_TMUX_PREFIX_KEY;

    @Test
    public void sendsTmuxPrefixThenCopyModeAndSearchBackwardForSelectedSessionName() {
        Assert.assertEquals(
            PREFIX + ":copy-mode -t 'umino'\n"
                + PREFIX + ":send-keys -t 'umino' -X search-backward '<call-to-user>'\n",
            HostTmuxCallToUserScrollCommand.forSessionName("umino"));
    }

    @Test
    public void prefixesEachLineWithControlBByDefaultSoTmuxInterceptsRegardlessOfForegroundProgram() {
        String command = HostTmuxCallToUserScrollCommand.forSessionName("umino");
        String[] lines = command.split("\n");
        Assert.assertEquals(2, lines.length);
        for (String line : lines) {
            Assert.assertEquals(0x02, line.charAt(0));
            Assert.assertEquals(':', line.charAt(1));
        }
    }

    @Test
    public void usesProvidedPrefixKeyWhenHostCustomizedTheTmuxPrefix() {
        char customPrefix = 0x01;
        Assert.assertEquals(
            customPrefix + ":copy-mode -t 'umino'\n"
                + customPrefix + ":send-keys -t 'umino' -X search-backward '<call-to-user>'\n",
            HostTmuxCallToUserScrollCommand.forSessionName("umino", customPrefix));
    }

    @Test
    public void shellQuotesSessionNameWithSingleQuote() {
        Assert.assertEquals(
            PREFIX + ":copy-mode -t 'it'\\''s'\n"
                + PREFIX + ":send-keys -t 'it'\\''s' -X search-backward '<call-to-user>'\n",
            HostTmuxCallToUserScrollCommand.forSessionName("it's"));
    }

    @Test
    public void quotesSessionNameContainingShellMetacharacters() {
        Assert.assertEquals(
            PREFIX + ":copy-mode -t 'a;b c$d'\n"
                + PREFIX + ":send-keys -t 'a;b c$d' -X search-backward '<call-to-user>'\n",
            HostTmuxCallToUserScrollCommand.forSessionName("a;b c$d"));
    }

    @Test
    public void returnsNullForNullSessionName() {
        Assert.assertNull(HostTmuxCallToUserScrollCommand.forSessionName(null));
    }

    @Test
    public void returnsNullForEmptySessionName() {
        Assert.assertNull(HostTmuxCallToUserScrollCommand.forSessionName(""));
    }
}
