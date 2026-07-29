package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class HostTmuxSessionNameTest {

    @Test
    public void dotsBecomeUnderscoresBecauseTmuxRewritesThem() {
        Assert.assertEquals("host_example_com", HostTmuxSessionName.normalize("host.example.com"));
    }

    @Test
    public void colonsBecomeUnderscoresBecauseTmuxRewritesThem() {
        Assert.assertEquals("host_8080", HostTmuxSessionName.normalize("host:8080"));
    }

    @Test
    public void namesWithoutDotsOrColonsAreUnchanged() {
        Assert.assertEquals("plain-session", HostTmuxSessionName.normalize("plain-session"));
    }

    @Test
    public void nullNameNormalizesToNull() {
        Assert.assertNull(HostTmuxSessionName.normalize(null));
    }

    @Test
    public void killCommandAndResetCommandNormalizeTheSameSessionNameIdentically() {
        String sessionName = "host.example.com:8080";
        String normalized = HostTmuxSessionName.normalize(sessionName);

        Assert.assertEquals("\u0002:kill-session -t '" + normalized + "'\n",
            HostTmuxSessionKillCommand.forSessionName(sessionName));
        Assert.assertEquals("reset.sh '" + normalized + "'",
            ResetSessionPlanner.plan("reset.sh {name}", sessionName).getCommand());
    }
}
