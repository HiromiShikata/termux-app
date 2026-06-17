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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ActiveSessionViewBindingInstrumentedTest {

    private static final String LOOPBACK_TAB_URL = "http://127.0.0.1/";

    private static final String PREVIOUS_SESSION_SHELL = "/system/bin/sh";

    @Test
    public void postSwitchGuardRebindsBothSurfacesToTheActiveSessionWhenTheBrowserShowsAForeignPage() {
        AtomicReference<String> activeHandleRef = new AtomicReference<>();
        AtomicReference<String> displayedTerminalHandleRef = new AtomicReference<>();
        AtomicReference<String> displayedBrowserHandleRef = new AtomicReference<>();

        try (ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class)) {
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
                setActiveSessionHandle(sessionClient, activeSession.mHandle);

                assertEquals(previousSession.mHandle, browserController.getDisplayedSessionHandle());

                invokeEnforceActiveSessionViewBinding(sessionClient, activeSession);

                activeHandleRef.set(activeSession.mHandle);
                displayedTerminalHandleRef.set(terminalView.getCurrentSession().mHandle);
                displayedBrowserHandleRef.set(browserController.getDisplayedSessionHandle());
            });
        }

        assertEquals(activeHandleRef.get(), displayedTerminalHandleRef.get());
        assertEquals(activeHandleRef.get(), displayedBrowserHandleRef.get());
    }

    private static TerminalSession newDetachedSession() {
        return new TerminalSession(PREVIOUS_SESSION_SHELL, "/", new String[]{PREVIOUS_SESSION_SHELL},
            new String[0], null, new TermuxTerminalSessionClientBase());
    }

    private static void setActiveSessionHandle(TermuxTerminalSessionActivityClient sessionClient,
                                               String activeSessionHandle) {
        try {
            Field field = TermuxTerminalSessionActivityClient.class.getDeclaredField("mActiveSessionHandle");
            field.setAccessible(true);
            field.set(sessionClient, activeSessionHandle);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new RuntimeException(reflectiveOperationException);
        }
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
