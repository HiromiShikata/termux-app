package com.termux.app;

import com.termux.app.apkupdate.UpdateTagUpdateController;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.SessionNewActivityTier;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that the service owns the explicit-call and app-update tag controllers and that feeding
 * them a session's output records the explicit call into the shared store and dispatches the update
 * flow, so detection works for non-current and backgrounded sessions without an activity being
 * bound.
 */
@RunWith(RobolectricTestRunner.class)
public class TermuxServiceOutputTagWiringTest {

    private ServiceController<TermuxService> controller;
    private TermuxService service;

    @Before
    public void setUp() {
        TermuxShellManager.init(RuntimeEnvironment.getApplication());
        controller = Robolectric.buildService(TermuxService.class).create();
        service = controller.get();
    }

    private static TerminalSession newTerminalSession() {
        return new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 100, null);
    }

    private static TermuxSession newTermuxSession(TerminalSession terminalSession) throws Exception {
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, null, null, false);
    }

    private TerminalSession addRunningSession(String sessionName) throws Exception {
        TerminalSession terminalSession = newTerminalSession();
        terminalSession.mSessionName = sessionName;
        Field shellManagerField = TermuxService.class.getDeclaredField("mShellManager");
        shellManagerField.setAccessible(true);
        TermuxShellManager shellManager = (TermuxShellManager) shellManagerField.get(service);
        shellManager.mTermuxSessions.add(newTermuxSession(terminalSession));
        return terminalSession;
    }

    @Test
    public void callToUserTagOnNonCurrentSessionRecordsRedTierIntoSharedStore() throws Exception {
        TerminalSession nonCurrentSession = addRunningSession("background-session");

        // No activity is bound, so this is the backgrounded path. Feeding the service-owned
        // controller the producing session's transcript must record the explicit call.
        service.getCallToUserTagController().onSessionTextChanged(
            nonCurrentSession.mHandle, "log <call-to-user>needs approval</call-to-user>");

        SessionNewActivityStore store = service.getSessionNewActivityStore();
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor("background-session"));
        Assert.assertEquals("needs approval", store.getLastExplicitCallReason("background-session"));
    }

    @Test
    public void updateTagDispatchesToRegisteredForegroundTrigger() throws Exception {
        TerminalSession session = addRunningSession("any-session");
        List<String> dispatchedReasons = new ArrayList<>();
        service.setUpdateTagReasonTrigger(dispatchedReasons::add);

        service.getUpdateTagUpdateController().onSessionTextChanged(
            session.mHandle, "<update-termux-app>new release</update-termux-app>");

        Assert.assertEquals(1, dispatchedReasons.size());
        Assert.assertEquals("new release", dispatchedReasons.get(0));
    }

    @Test
    public void updateTagIsDetectedButDoesNotDispatchWhenNoForegroundTriggerRegistered() throws Exception {
        TerminalSession session = addRunningSession("any-session");
        // No trigger registered: the app is backgrounded with no activity, so the install dialog
        // cannot run. The scan still completes without throwing and consumes the tag.
        UpdateTagUpdateController updateController = service.getUpdateTagUpdateController();

        updateController.onSessionTextChanged(
            session.mHandle, "<update-termux-app>new release</update-termux-app>");

        // After backgrounded detection consumed the tag, a foreground trigger registered later does
        // not re-fire the same already-consumed tag on a re-scan of the same transcript.
        List<String> dispatchedReasons = new ArrayList<>();
        service.setUpdateTagReasonTrigger(dispatchedReasons::add);
        updateController.onSessionTextChanged(
            session.mHandle, "<update-termux-app>new release</update-termux-app>");

        Assert.assertTrue(dispatchedReasons.isEmpty());
    }

    @Test
    public void callToUserTagWithUnknownHandleRecordsNothing() {
        service.getCallToUserTagController().onSessionTextChanged(
            "unknown-handle", "<call-to-user>needs approval</call-to-user>");

        SessionNewActivityStore store = service.getSessionNewActivityStore();
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor("unknown-handle"));
    }
}
