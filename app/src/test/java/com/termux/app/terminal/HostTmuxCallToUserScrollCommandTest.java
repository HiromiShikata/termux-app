package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxCallToUserScrollCommandTest {

    @Test
    public void issuesCopyModeAndSearchBackwardForSelectedSessionName() {
        Assert.assertEquals(
            "tmux copy-mode -t 'umino'\n"
                + "tmux send-keys -t 'umino' -X search-backward '<call-to-user>'\n",
            HostTmuxCallToUserScrollCommand.forSessionName("umino"));
    }

    @Test
    public void shellQuotesSessionNameWithSingleQuote() {
        Assert.assertEquals(
            "tmux copy-mode -t 'it'\\''s'\n"
                + "tmux send-keys -t 'it'\\''s' -X search-backward '<call-to-user>'\n",
            HostTmuxCallToUserScrollCommand.forSessionName("it's"));
    }

    @Test
    public void quotesSessionNameContainingShellMetacharacters() {
        Assert.assertEquals(
            "tmux copy-mode -t 'a;b c$d'\n"
                + "tmux send-keys -t 'a;b c$d' -X search-backward '<call-to-user>'\n",
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
