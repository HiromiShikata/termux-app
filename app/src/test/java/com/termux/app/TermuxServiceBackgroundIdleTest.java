package com.termux.app;

import android.content.Intent;
import android.os.PowerManager;

import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
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
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxServiceBackgroundIdleTest {

    private ServiceController<TermuxService> controller;
    private TermuxService service;

    @Before
    public void setUp() throws Exception {
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

    private void addRunningSession() throws Exception {
        Field shellManagerField = TermuxService.class.getDeclaredField("mShellManager");
        shellManagerField.setAccessible(true);
        TermuxShellManager shellManager = (TermuxShellManager) shellManagerField.get(service);
        shellManager.mTermuxSessions.add(newTermuxSession(newTerminalSession()));
    }

    @SuppressWarnings("unchecked")
    private List<TermuxSession> runningSessions() throws Exception {
        Field shellManagerField = TermuxService.class.getDeclaredField("mShellManager");
        shellManagerField.setAccessible(true);
        TermuxShellManager shellManager = (TermuxShellManager) shellManagerField.get(service);
        return shellManager.mTermuxSessions;
    }

    private PowerManager.WakeLock wakeLock() throws Exception {
        Field wakeLockField = TermuxService.class.getDeclaredField("mWakeLock");
        wakeLockField.setAccessible(true);
        return (PowerManager.WakeLock) wakeLockField.get(service);
    }

    private void sendAction(String action) {
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), TermuxService.class).setAction(action);
        service.onStartCommand(intent, 0, 1);
    }

    @Test
    public void releasesWakeLockOnBackgroundAndReacquiresOnForeground() throws Exception {
        addRunningSession();

        sendAction(TERMUX_SERVICE.ACTION_WAKE_LOCK);
        Assert.assertNotNull("Wake lock must be held after ACTION_WAKE_LOCK", wakeLock());
        Assert.assertTrue(wakeLock().isHeld());

        service.onActivityBackgrounded();
        Assert.assertNull("Wake lock must be auto-released when activity is backgrounded", wakeLock());
        Assert.assertEquals("Running session must not be killed when backgrounded", 1, runningSessions().size());
        Assert.assertTrue("Running session process must still be alive when backgrounded",
            runningSessions().get(0).getTerminalSession().isRunning());

        service.onActivityForegrounded();
        Assert.assertNotNull("Wake lock must be re-acquired when activity returns to foreground", wakeLock());
        Assert.assertTrue(wakeLock().isHeld());
    }

    @Test
    public void doesNotReacquireWakeLockManuallyReleasedWhileBackgrounded() throws Exception {
        addRunningSession();

        sendAction(TERMUX_SERVICE.ACTION_WAKE_LOCK);
        Assert.assertNotNull(wakeLock());

        sendAction(TERMUX_SERVICE.ACTION_WAKE_UNLOCK);
        Assert.assertNull("Wake lock must be released after manual ACTION_WAKE_UNLOCK", wakeLock());

        service.onActivityBackgrounded();
        service.onActivityForegrounded();
        Assert.assertNull("Manually released wake lock must not be re-acquired on foreground", wakeLock());
    }

    @Test
    public void doesNotReacquireWakeLockThatWasNeverHeld() throws Exception {
        addRunningSession();

        service.onActivityBackgrounded();
        Assert.assertNull(wakeLock());

        service.onActivityForegrounded();
        Assert.assertNull("Wake lock must not be acquired on foreground when it was never held", wakeLock());
    }
}
