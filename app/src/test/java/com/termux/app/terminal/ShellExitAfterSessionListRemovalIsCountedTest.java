package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.diagnostics.DiagnosticEvent;
import com.termux.app.diagnostics.DiagnosticEventLogHolder;
import com.termux.app.diagnostics.DiagnosticEventType;
import com.termux.app.diagnostics.DiagnosticsShellExitCount;
import com.termux.app.diagnostics.ShellExitStatusRecorderHolder;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ShellExitAfterSessionListRemovalIsCountedTest {

    private static final String DISPLAYED_SESSION_NAME = "displayed-session";

    private static final String REMOVED_SESSION_NAME = "removed-session";

    private static final int EXIT_STATUS_OF_A_KILLED_SHELL = 255;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setSessionDefinitionMaxSessions(10);
        preferences.setPersistedSessions("");

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession displayedSession = diedSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        terminalView.mTermSession = displayedSession.getTerminalSession();
    }

    @Test
    public void aShellThatEndsAfterItsSessionLeftTheListIsStillCounted() throws Exception {
        TermuxSession removedSession = diedSession(REMOVED_SESSION_NAME);
        shellManager.mTermuxSessions.add(removedSession);
        TerminalSession removedTerminalSession = removedSession.getTerminalSession();
        service.removeTermuxSession(removedTerminalSession);

        int countedBefore = totalShellExitsCounted();
        activity.getTermuxTerminalSessionClient().onSessionFinished(removedTerminalSession);

        assertEquals("a reconnect removes the session from the service list before its shell process"
                + " finishes ending, so an exit that is counted only while the session is still listed"
                + " leaves every reconnected session invisible to the exit counter",
            countedBefore + 1, totalShellExitsCounted());
    }

    @Test
    public void aShellEndingIsCountedUnderTheStatusTheShellActuallyExitedWith() throws Exception {
        TermuxSession removedSession = diedSession(REMOVED_SESSION_NAME);
        shellManager.mTermuxSessions.add(removedSession);
        TerminalSession removedTerminalSession = removedSession.getTerminalSession();
        setExitStatus(removedTerminalSession, EXIT_STATUS_OF_A_KILLED_SHELL);
        service.removeTermuxSession(removedTerminalSession);

        int countedBefore = countedUnderExitStatus(EXIT_STATUS_OF_A_KILLED_SHELL);
        activity.getTermuxTerminalSessionClient().onSessionFinished(removedTerminalSession);

        assertEquals("an exit that is counted without its status cannot separate a killed shell from"
                + " one that returned on its own, which is the distinction the report exists to draw",
            countedBefore + 1, countedUnderExitStatus(EXIT_STATUS_OF_A_KILLED_SHELL));
    }

    @Test
    public void aShellEndingWhileItsSessionIsStillListedIsCountedExactlyOnce() throws Exception {
        TermuxSession listedSession = diedSession(REMOVED_SESSION_NAME);
        shellManager.mTermuxSessions.add(listedSession);

        int countedBefore = totalShellExitsCounted();
        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(listedSession.getTerminalSession());

        assertEquals("one shell process ending is one exit, so a session still in the list must not be"
                + " counted twice by the removal that follows its ending",
            countedBefore + 1, totalShellExitsCounted());
    }

    @Test
    public void aShellEndingObservedWhileTheActivityIsUnboundIsCountedTheSameWay() throws Exception {
        TermuxSession backgroundSession = diedSession(REMOVED_SESSION_NAME);

        int countedBefore = totalShellExitsCounted();
        new TermuxTerminalSessionServiceClient(service)
            .onSessionFinished(backgroundSession.getTerminalSession());

        assertEquals("shells keep ending while the app is backgrounded and the activity is unbound, so"
                + " an exit counted only by the activity client undercounts exactly the window the"
                + " owner cannot watch",
            countedBefore + 1, totalShellExitsCounted());
    }

    @Test
    public void aShellEndingIsCountedEvenWhenTheServiceIsAlreadyGone() throws Exception {
        TermuxSession dyingSession = diedSession(REMOVED_SESSION_NAME);
        set(activity, TermuxActivity.class, "mTermuxService", null);

        int countedBefore = totalShellExitsCounted();
        activity.getTermuxTerminalSessionClient()
            .onSessionFinished(dyingSession.getTerminalSession());

        assertEquals("the shell of a session whose service is already stopping still ends, and an exit"
                + " the counter drops because the service reference went away first is the same silent"
                + " loss under a different cause",
            countedBefore + 1, totalShellExitsCounted());
    }

    @Test
    public void aShellEndingAfterRemovalLeavesAnExitEventBehindInTheEventLog() throws Exception {
        TermuxSession removedSession = diedSession(REMOVED_SESSION_NAME);
        shellManager.mTermuxSessions.add(removedSession);
        TerminalSession removedTerminalSession = removedSession.getTerminalSession();
        service.removeTermuxSession(removedTerminalSession);

        int recordedBefore = exitEventsNaming(REMOVED_SESSION_NAME);
        activity.getTermuxTerminalSessionClient().onSessionFinished(removedTerminalSession);

        assertEquals("the count says how many shells ended and the event log says which ones and when,"
                + " so an ending that leaves no event cannot be placed against the session movement it"
                + " belongs to",
            recordedBefore + 1, exitEventsNaming(REMOVED_SESSION_NAME));
    }

    @Test
    public void aRemovalFollowedByTheEndingOfThatSameShellCountsOneExitBetweenThem() throws Exception {
        TermuxSession removedSession = diedSession(REMOVED_SESSION_NAME);
        shellManager.mTermuxSessions.add(removedSession);
        TerminalSession removedTerminalSession = removedSession.getTerminalSession();

        int countedBefore = totalShellExitsCounted();
        service.removeTermuxSession(removedTerminalSession);
        activity.getTermuxTerminalSessionClient().onSessionFinished(removedTerminalSession);

        assertEquals("the removal and the ending are two observations of one shell process, so counting"
                + " at both would turn the undercount into an overcount",
            countedBefore + 1, totalShellExitsCounted());
    }

    private int totalShellExitsCounted() {
        return ShellExitStatusRecorderHolder.getInstance().snapshot().getTotalExitCount();
    }

    private int countedUnderExitStatus(int exitStatus) {
        List<DiagnosticsShellExitCount> countsByExitStatus =
            ShellExitStatusRecorderHolder.getInstance().snapshot().getCountsByExitStatus();
        for (DiagnosticsShellExitCount countByExitStatus : countsByExitStatus) {
            if (countByExitStatus.getExitStatus() == exitStatus) {
                return countByExitStatus.getCount();
            }
        }
        return 0;
    }

    private int exitEventsNaming(String sessionName) {
        int recorded = 0;
        List<DiagnosticEvent> events =
            DiagnosticEventLogHolder.getInstance().tail(DiagnosticEventLogHolder.getInstance().size());
        for (DiagnosticEvent event : events) {
            if (event.getType() == DiagnosticEventType.SESSION_EXITED
                && event.getDetail().startsWith(sessionName)) {
                recorded++;
            }
        }
        return recorded;
    }

    private static final int PID_OF_A_SHELL_PROCESS_THAT_ENDED = -1;

    private TermuxSession diedSession(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, PID_OF_A_SHELL_PROCESS_THAT_ENDED);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), service, false);
    }

    private void setExitStatus(TerminalSession terminalSession, int exitStatus) throws Exception {
        Field shellExitStatus = TerminalSession.class.getDeclaredField("mShellExitStatus");
        shellExitStatus.setAccessible(true);
        shellExitStatus.setInt(terminalSession, exitStatus);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
