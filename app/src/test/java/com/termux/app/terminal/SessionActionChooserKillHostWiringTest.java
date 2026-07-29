package com.termux.app.terminal;

import com.termux.R;
import com.termux.app.terminal.TermuxSessionsListViewController.SessionAction;

import org.junit.Assert;
import org.junit.Test;

public class SessionActionChooserKillHostWiringTest {

    @Test
    public void killHostSessionActionSitsBetweenHideAndResetSession() {
        Assert.assertEquals(SessionAction.HIDE, SessionAction.atIndex(2));
        Assert.assertEquals(SessionAction.KILL_HOST_SESSION, SessionAction.atIndex(3));
        Assert.assertEquals(SessionAction.RESET_SESSION, SessionAction.atIndex(4));
    }

    @Test
    public void killHostSessionActionUsesKillHostSessionLabel() {
        Assert.assertEquals(R.string.action_kill_host_session,
            SessionAction.KILL_HOST_SESSION.labelResId);
    }

    @Test
    public void killHostSessionActionIssuesHostSideKillForSelectedSessionName() {
        String selectedSessionName = SessionAction.atIndex(3) == SessionAction.KILL_HOST_SESSION
            ? "host-session" : null;
        Assert.assertEquals(":kill-session -t 'host-session'\n",
            HostTmuxSessionKillCommand.forSessionName(selectedSessionName));
    }
}
