package com.termux.app.ownercall;

import com.termux.app.terminal.HostTmuxSessionKillCommand;
import com.termux.app.terminal.HostTmuxSessionName;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallFilePathTest {

    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";

    @Test
    public void filesACallUnderItsProjectCodeWithEverySlashOfTheSessionNameReplaced() {
        Assert.assertEquals(
            "call-to-user/umino/https___github_com_HiromiShikata_termux-app_issues_1884.yaml",
            OwnerCallFilePath.of("umino", HostTmuxSessionName.normalize(SESSION_URL)));
    }

    @Test
    public void filesACallOfASessionThatBelongsToNoProjectUnderNA() {
        Assert.assertEquals("call-to-user/NA/secretary.yaml",
            OwnerCallFilePath.of(null, HostTmuxSessionName.normalize("secretary")));
    }

    @Test
    public void keysTheFileByTheSameSessionNameTheHostTmuxCommandsTarget() {
        String hostTmuxSessionName = HostTmuxSessionName.normalize(SESSION_URL);

        Assert.assertTrue("the host commands address the session by the shared normalization",
            HostTmuxSessionKillCommand.forSessionName(SESSION_URL,
                "ssh host tmux kill-session -t {name}").contains("'" + hostTmuxSessionName + "'"));
        Assert.assertEquals("call-to-user/umino/" + hostTmuxSessionName.replace('/', '_') + ".yaml",
            OwnerCallFilePath.of("umino", hostTmuxSessionName));
    }

    @Test
    public void treatsAnEmptyProjectCodeAsNoProject() {
        Assert.assertEquals(OwnerCallFilePath.of(null, "app"), OwnerCallFilePath.of("", "app"));
    }
}
