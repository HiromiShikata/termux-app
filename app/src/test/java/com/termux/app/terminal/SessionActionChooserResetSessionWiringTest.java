package com.termux.app.terminal;

import com.termux.R;
import com.termux.app.terminal.TermuxSessionsListViewController.SessionAction;

import org.junit.Assert;
import org.junit.Test;

public class SessionActionChooserResetSessionWiringTest {

    @Test
    public void resetSessionActionSitsBetweenKillHostSessionAndDelete() {
        Assert.assertEquals(SessionAction.KILL_HOST_SESSION, SessionAction.atIndex(3));
        Assert.assertEquals(SessionAction.RESET_SESSION, SessionAction.atIndex(4));
        Assert.assertEquals(SessionAction.DELETE, SessionAction.atIndex(5));
    }

    @Test
    public void resetSessionActionUsesResetSessionLabel() {
        Assert.assertEquals(R.string.action_reset_session, SessionAction.RESET_SESSION.labelResId);
    }

    @Test
    public void resetSessionActionComposesConfiguredTemplateForSelectedSessionName() {
        String selectedSessionName = SessionAction.atIndex(4) == SessionAction.RESET_SESSION
            ? "host.example.com" : null;
        Assert.assertEquals("ssh gateway /opt/reset.sh 'host_example_com'",
            ResetSessionCommand.forTemplateAndSessionName(
                "ssh gateway /opt/reset.sh {name}", selectedSessionName));
    }

    @Test
    public void resetSessionActionSurfacesNotConfiguredStateForEmptyTemplate() {
        String selectedSessionName = SessionAction.atIndex(4) == SessionAction.RESET_SESSION
            ? "host.example.com" : null;
        Assert.assertNull(ResetSessionCommand.forTemplateAndSessionName("", selectedSessionName));
    }
}
