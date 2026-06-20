package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

@RunWith(AndroidJUnit4.class)
public class ActiveSessionViewBindingInstrumentedTest {

    private static final String LOOPBACK_TAB_URL = "http://127.0.0.1/";

    private static final String DETACHED_SESSION_SHELL = "/system/bin/sh";

    @Test
    public void postSwitchGuardRebindsBothSurfacesToTheActiveSessionWhenTheBrowserShowsAForeignPage() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxTerminalSessionActivityClient sessionClient = activity.getTermuxTerminalSessionClient();
            TermuxBrowserController browserController = activity.getTermuxBrowserController();
            TerminalView terminalView = activity.getTerminalView();
            assertNotNull(sessionClient);
            assertNotNull(browserController);
            assertNotNull(terminalView);

            TerminalSession previousSession = newDetachedSession();
            TerminalSession activeSession = newDetachedSession();

            browserController.onSessionChanged(previousSession);
            browserController.openUrlInNewTab(LOOPBACK_TAB_URL);
            browserController.attachBackgroundTab(activeSession.mHandle, LOOPBACK_TAB_URL);

            terminalView.mTermSession = activeSession;

            assertEquals(previousSession.mHandle, browserController.getDisplayedSessionHandle());

            invokeEnforceActiveSessionViewBinding(sessionClient, activeSession);

            assertEquals(activeSession.mHandle, terminalView.getCurrentSession().mHandle);
            assertEquals(activeSession.mHandle, browserController.getDisplayedSessionHandle());
        });
    }

    private static TerminalSession newDetachedSession() {
        return new TerminalSession(DETACHED_SESSION_SHELL, "/", new String[]{DETACHED_SESSION_SHELL},
            new String[0], null, new TermuxTerminalSessionClientBase());
    }

    private static void invokeEnforceActiveSessionViewBinding(
        TermuxTerminalSessionActivityClient sessionClient, TerminalSession activeSession) {
        try {
            Method method = TermuxTerminalSessionActivityClient.class.getDeclaredMethod(
                "enforceActiveSessionViewBinding", TerminalSession.class);
            method.setAccessible(true);
            method.invoke(sessionClient, activeSession);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new RuntimeException(reflectiveOperationException);
        }
    }
}
