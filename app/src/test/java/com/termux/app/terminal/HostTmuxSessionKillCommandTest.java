package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxSessionKillCommandTest {

    private static final String KILL_TEMPLATE = "ssh host tmux kill-session -t {name}";

    @Test
    public void substitutesShellQuotedSessionNameIntoStoredTemplate() {
        Assert.assertEquals("ssh host tmux kill-session -t 'host-session'",
            HostTmuxSessionKillCommand.forSessionName("host-session", KILL_TEMPLATE));
    }

    @Test
    public void normalizesDotsInSessionNameToUnderscoresLikeTmuxDoesAtSessionCreation() {
        Assert.assertEquals("ssh host tmux kill-session -t 'github_com'",
            HostTmuxSessionKillCommand.forSessionName("github.com", KILL_TEMPLATE));
    }

    @Test
    public void normalizesColonsInSessionNameToUnderscoresLikeTmuxDoesAtSessionCreation() {
        Assert.assertEquals("ssh host tmux kill-session -t 'https_//github_com/owner/repo/issues/123'",
            HostTmuxSessionKillCommand.forSessionName("https://github.com/owner/repo/issues/123", KILL_TEMPLATE));
    }

    @Test
    public void posixQuotesSessionNameContainingASingleQuote() {
        Assert.assertEquals("ssh host tmux kill-session -t 'it'\\''s'",
            HostTmuxSessionKillCommand.forSessionName("it's", KILL_TEMPLATE));
    }

    @Test
    public void posixQuotesSessionNameContainingShellMetacharacters() {
        Assert.assertEquals("ssh host tmux kill-session -t 'a;b c$d'",
            HostTmuxSessionKillCommand.forSessionName("a;b c$d", KILL_TEMPLATE));
    }

    @Test
    public void substitutesEveryOccurrenceOfThePlaceholder() {
        Assert.assertEquals("tmux has-session -t 'host-session' && tmux kill-session -t 'host-session'",
            HostTmuxSessionKillCommand.forSessionName("host-session",
                "tmux has-session -t {name} && tmux kill-session -t {name}"));
    }

    @Test
    public void trimsSurroundingWhitespaceOfTheStoredTemplate() {
        Assert.assertEquals("ssh host tmux kill-session -t 'host-session'",
            HostTmuxSessionKillCommand.forSessionName("host-session", "  " + KILL_TEMPLATE + "\n"));
    }

    @Test
    public void producesNoCommandWhenTemplateIsNull() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("host-session", null));
    }

    @Test
    public void producesNoCommandWhenTemplateIsEmpty() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("host-session", ""));
    }

    @Test
    public void producesNoCommandWhenTemplateIsOnlyWhitespace() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("host-session", "   \n"));
    }

    @Test
    public void producesNoCommandWhenSessionNameIsNull() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName(null, KILL_TEMPLATE));
    }

    @Test
    public void producesNoCommandWhenSessionNameIsEmpty() {
        Assert.assertNull(HostTmuxSessionKillCommand.forSessionName("", KILL_TEMPLATE));
    }

    @Test
    public void reportsWhetherAStoredTemplateIsConfigured() {
        Assert.assertFalse(HostTmuxSessionKillCommand.hasCommandTemplate(null));
        Assert.assertFalse(HostTmuxSessionKillCommand.hasCommandTemplate(""));
        Assert.assertFalse(HostTmuxSessionKillCommand.hasCommandTemplate("  \n "));
        Assert.assertTrue(HostTmuxSessionKillCommand.hasCommandTemplate(KILL_TEMPLATE));
    }

    @Test
    public void targetsExactlyTheNameProducedBySharedHostTmuxSessionNameNormalization() {
        String sessionName = "host.example.com:8080";

        Assert.assertTrue(HostTmuxSessionKillCommand.forSessionName(sessionName, KILL_TEMPLATE)
            .contains("'" + HostTmuxSessionName.normalize(sessionName) + "'"));
    }

    @Test
    public void neverEmitsATmuxPrefixKeyBecauseTheCommandIsNotTypedIntoTheAttachedSession() {
        String command = HostTmuxSessionKillCommand.forSessionName("host-session", KILL_TEMPLATE);
        Assert.assertFalse("a tmux prefix byte would only be meaningful when typed into the attached pty",
            command.indexOf(0x02) >= 0);
        Assert.assertFalse("a trailing terminator would only be meaningful when typed into the attached pty",
            command.endsWith("\n") || command.endsWith("\r"));
    }
}
