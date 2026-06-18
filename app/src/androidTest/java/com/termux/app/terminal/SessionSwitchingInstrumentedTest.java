package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SessionSwitchingInstrumentedTest {

    private static final String DETACHED_SESSION_SHELL = "/system/bin/sh";

    @Test
    public void setCurrentSessionSwitchesTheActiveSessionShownByTheTerminalView() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient sessionClient = activity.getTermuxTerminalSessionClient();
            TerminalView terminalView = activity.getTerminalView();
            assertNotNull(sessionClient);
            assertNotNull(terminalView);

            TerminalSession firstSession = newDetachedSession();
            TerminalSession secondSession = newDetachedSession();

            sessionClient.setCurrentSession(firstSession);
            assertEquals(firstSession.mHandle, terminalView.getCurrentSession().mHandle);
            assertEquals(firstSession.mHandle, activity.getCurrentSession().mHandle);

            sessionClient.setCurrentSession(secondSession);
            assertEquals(secondSession.mHandle, terminalView.getCurrentSession().mHandle);
            assertEquals(secondSession.mHandle, activity.getCurrentSession().mHandle);

            sessionClient.setCurrentSession(firstSession);
            assertEquals(firstSession.mHandle, terminalView.getCurrentSession().mHandle);
            assertEquals(firstSession.mHandle, activity.getCurrentSession().mHandle);
        });
    }

    private static TerminalSession newDetachedSession() {
        return new TerminalSession(DETACHED_SESSION_SHELL, "/", new String[]{DETACHED_SESSION_SHELL},
            new String[0], null, new TermuxTerminalSessionClientBase());
    }
}
