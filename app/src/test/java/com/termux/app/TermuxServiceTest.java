package com.termux.app;

import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TermuxServiceTest {

    private static TerminalSession newTerminalSession() throws Exception {
        TerminalSession terminalSession =
            new TerminalSession("/system/bin/sh", "/", new String[0], new String[0], 100, null);
        markShellProcessAsRunning(terminalSession);
        return terminalSession;
    }

    private static final int RUNNING_SHELL_PROCESS_PID = 1;

    private static void markShellProcessAsRunning(TerminalSession terminalSession) throws Exception {
        Field shellPidField = TerminalSession.class.getDeclaredField("mShellPid");
        shellPidField.setAccessible(true);
        shellPidField.setInt(terminalSession, RUNNING_SHELL_PROCESS_PID);
    }

    private static TermuxSession newTermuxSession(TerminalSession terminalSession) throws Exception {
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, null, null, false);
    }

    @Test
    public void removesSessionWhoseProcessIsStillRunningInOneStep() throws Exception {
        TerminalSession runningSession = newTerminalSession();
        Assert.assertTrue(runningSession.isRunning());

        List<TermuxSession> termuxSessions = new ArrayList<>();
        termuxSessions.add(newTermuxSession(runningSession));

        boolean removed = TermuxService.removeFromTermuxSessions(termuxSessions, runningSession);

        Assert.assertTrue(removed);
        Assert.assertTrue(termuxSessions.isEmpty());
    }

    @Test
    public void removesOnlyTheTargetedSessionAndKeepsTheOthers() throws Exception {
        TerminalSession firstSession = newTerminalSession();
        TerminalSession secondSession = newTerminalSession();
        TerminalSession thirdSession = newTerminalSession();

        List<TermuxSession> termuxSessions = new ArrayList<>();
        TermuxSession firstTermuxSession = newTermuxSession(firstSession);
        TermuxSession thirdTermuxSession = newTermuxSession(thirdSession);
        termuxSessions.add(firstTermuxSession);
        termuxSessions.add(newTermuxSession(secondSession));
        termuxSessions.add(thirdTermuxSession);

        boolean removed = TermuxService.removeFromTermuxSessions(termuxSessions, secondSession);

        Assert.assertTrue(removed);
        Assert.assertEquals(2, termuxSessions.size());
        Assert.assertSame(firstTermuxSession, termuxSessions.get(0));
        Assert.assertSame(thirdTermuxSession, termuxSessions.get(1));
    }

    @Test
    public void returnsFalseWhenTheSessionIsNotInTheList() throws Exception {
        List<TermuxSession> termuxSessions = new ArrayList<>();
        termuxSessions.add(newTermuxSession(newTerminalSession()));

        boolean removed = TermuxService.removeFromTermuxSessions(termuxSessions, newTerminalSession());

        Assert.assertFalse(removed);
        Assert.assertEquals(1, termuxSessions.size());
    }
}
