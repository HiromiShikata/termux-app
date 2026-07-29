package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ResetSessionCommandTest {

    @Test
    public void placeholderIsReplacedWithQuotedSessionName() {
        Assert.assertEquals("ssh host /opt/reset.sh 'work-session'",
            ResetSessionCommand.forTemplateAndSessionName("ssh host /opt/reset.sh {name}", "work-session"));
    }

    @Test
    public void dotsInSessionNameAreNormalizedToUnderscoresBeforeQuoting() {
        Assert.assertEquals("ssh host /opt/reset.sh 'host_example_com'",
            ResetSessionCommand.forTemplateAndSessionName("ssh host /opt/reset.sh {name}", "host.example.com"));
    }

    @Test
    public void singleQuoteInSessionNameIsEscapedForPosixShells() {
        Assert.assertEquals("ssh host /opt/reset.sh 'it'\\''s'",
            ResetSessionCommand.forTemplateAndSessionName("ssh host /opt/reset.sh {name}", "it's"));
    }

    @Test
    public void everyPlaceholderOccurrenceIsReplaced() {
        Assert.assertEquals("echo 'a_b' && reset 'a_b'",
            ResetSessionCommand.forTemplateAndSessionName("echo {name} && reset {name}", "a.b"));
    }

    @Test
    public void emptyTemplateProducesNoCommand() {
        Assert.assertNull(ResetSessionCommand.forTemplateAndSessionName("", "work-session"));
    }

    @Test
    public void blankTemplateProducesNoCommand() {
        Assert.assertNull(ResetSessionCommand.forTemplateAndSessionName("   \n  ", "work-session"));
    }

    @Test
    public void nullTemplateProducesNoCommand() {
        Assert.assertNull(ResetSessionCommand.forTemplateAndSessionName(null, "work-session"));
    }

    @Test
    public void missingSessionNameProducesNoCommand() {
        Assert.assertNull(ResetSessionCommand.forTemplateAndSessionName("reset.sh {name}", null));
        Assert.assertNull(ResetSessionCommand.forTemplateAndSessionName("reset.sh {name}", ""));
    }

    @Test
    public void resetSessionRunsUnderANameDistinctFromTheSessionBeingReset() {
        Assert.assertEquals("reset work-session", ResetSessionCommand.sessionNameFor("work-session"));
    }
}
