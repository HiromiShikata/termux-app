package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalEmulator;
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
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
public class HiddenCurrentSessionRuntimeReleaseTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String OTHER_SESSION_NAME = "session-other";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int RUNNING_SHELL_PROCESS_PID = 1;

    private static final byte[] OWNER_KEYSTROKE = new byte[]{'l', 's', '\r'};

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TerminalView terminalView;
    private TermuxSessionsListViewController listViewController;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");

        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mIsVisible", true);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        terminalView.setTextSize(12);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        shellManager.mTermuxSessions.add(runningSessionHoldingAnEmulator(CURRENT_SESSION_NAME));
        terminalView.mTermSession = terminalSessionNamed(CURRENT_SESSION_NAME);

        listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
    }

    @Test
    public void hidingTheDisplayedSessionMovesTheTerminalViewToTheNextVisibleSessionAndReleasesOnlyTheHiddenOne()
        throws Exception {
        shellManager.mTermuxSessions.add(runningSessionHoldingAnEmulator(OTHER_SESSION_NAME));
        TerminalSession sessionBeingHidden = terminalSessionNamed(CURRENT_SESSION_NAME);

        assertEquals("test premise: the session about to be hidden must be the one the terminal view "
                + "renders, otherwise this test is not exercising the hide of the displayed session",
            CURRENT_SESSION_NAME, terminalView.getCurrentSession().mSessionName);

        hideThroughTheSessionListHideAction(CURRENT_SESSION_NAME);

        assertEquals("hiding the displayed session must attach the terminal view to the next visible "
                + "session, because a view left attached to a released session renders content that no "
                + "longer has a session behind it",
            OTHER_SESSION_NAME, terminalView.getCurrentSession().mSessionName);
        assertNull("the session the owner hid must release its terminal emulator and the scrollback "
                + "buffer it owns", sessionBeingHidden.getEmulator());
        assertFalse("the session the owner hid must hold no shell process",
            sessionBeingHidden.isRunning());
        assertNull("the session the owner hid must hold no live session object either",
            service.getTermuxSessionForSessionName(CURRENT_SESSION_NAME));
        assertNotNull("the session the terminal view moved to must keep its terminal emulator",
            terminalSessionNamed(OTHER_SESSION_NAME).getEmulator());
        assertTrue("the session the terminal view moved to must keep its shell process",
            terminalSessionNamed(OTHER_SESSION_NAME).isRunning());
        assertTrue("a keystroke typed after the hide must reach the shell process of the session the "
                + "terminal view moved to, and delivering zero bytes means typing does nothing at all",
            keystrokeByteCountDeliveredTo(terminalSessionNamed(OTHER_SESSION_NAME)) > 0);
    }

    @Test
    public void hidingTheLastVisibleSessionMustNotTakeEffectAtAll() throws Exception {
        assertEquals("test premise: the session about to be hidden must be the only one, so no session "
                + "is left for the terminal view to move to", 1, service.getTermuxSessionsSize());

        hideThroughTheSessionListHideAction(CURRENT_SESSION_NAME);

        assertEquals("with no session left to move the terminal view to, the hide must not take effect, "
                + "so the terminal view must still be attached to that session",
            CURRENT_SESSION_NAME, terminalView.getCurrentSession().mSessionName);
        assertFalse("a hide that cannot take effect must not leave the session marked hidden in stored "
                + "state, otherwise the row is treated as hidden by every later scan while the owner is "
                + "still looking at it",
            preferences.isSessionDisabled(CURRENT_SESSION_NAME));
        assertNotNull("the last visible session must keep its terminal emulator and the scrollback "
                + "buffer it owns", terminalSessionNamed(CURRENT_SESSION_NAME).getEmulator());
        assertTrue("the last visible session must keep its shell process",
            terminalSessionNamed(CURRENT_SESSION_NAME).isRunning());
        assertTrue("a keystroke typed into the last visible session must reach its shell process, and "
                + "delivering zero bytes means the owner is looking at a terminal that silently "
                + "swallows everything typed into it",
            keystrokeByteCountDeliveredTo(terminalSessionNamed(CURRENT_SESSION_NAME)) > 0);
    }

    private void hideThroughTheSessionListHideAction(String sessionName) throws Exception {
        Method hideSession = TermuxSessionsListViewController.class.getDeclaredMethod(
            "hideSession", String.class);
        hideSession.setAccessible(true);
        hideSession.invoke(listViewController, sessionName);
    }

    private static int keystrokeByteCountDeliveredTo(TerminalSession terminalSession) throws Exception {
        terminalSession.write(OWNER_KEYSTROKE, 0, OWNER_KEYSTROKE.length);
        Field ioQueueField = TerminalSession.class.getDeclaredField("mTerminalToProcessIOQueue");
        ioQueueField.setAccessible(true);
        Object ioQueue = ioQueueField.get(terminalSession);
        assertNotNull(ioQueue);
        Field storedBytesField = ioQueue.getClass().getDeclaredField("mStoredBytes");
        storedBytesField.setAccessible(true);
        return storedBytesField.getInt(ioQueue);
    }

    private TerminalSession terminalSessionNamed(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull(termuxSession);
        return termuxSession.getTerminalSession();
    }

    private TermuxSession runningSessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, RUNNING_SHELL_PROCESS_PID);
        Field emulator = TerminalSession.class.getDeclaredField("mEmulator");
        emulator.setAccessible(true);
        emulator.set(terminalSession, new TerminalEmulator(terminalSession, TERMINAL_COLUMNS,
            TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient()));
        return termuxSession(terminalSession);
    }

    private static TermuxSession termuxSession(TerminalSession terminalSession) throws Exception {
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
        throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
