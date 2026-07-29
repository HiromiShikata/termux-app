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
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenCurrentSessionReplacementSelectionTest {

    private static final String COLLAPSED_GROUP_SESSION_NAME = "session-inside-collapsed-group";

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String ON_SCREEN_SESSION_NAME = "session-on-screen";

    private static final String COLLAPSED_PROJECT_LABEL = "project-collapsed";

    private static final String ON_SCREEN_PROJECT_LABEL = "project-on-screen";

    private static final String COLLAPSED_STORY_LABEL = "story-inside-collapsed-group";

    private static final String ON_SCREEN_STORY_LABEL = "story-on-screen";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int RUNNING_SHELL_PROCESS_PID = 1;

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
    }

    @Test
    public void hidingTheDisplayedSessionMustNotMoveTheTerminalViewIntoAProjectGroupTheOwnerCollapsed()
        throws Exception {
        buildSessionList(true);
        collapseThroughTheRealProjectHeaderClick(COLLAPSED_PROJECT_LABEL);

        assertEquals("test premise: the session inside the collapsed group must be the one the topmost "
                + "non-hidden selection reaches first, otherwise this test is not exercising the "
                + "collapsed-group case at all",
            COLLAPSED_GROUP_SESSION_NAME,
            sessionNameAtIndex(listViewController.getTopmostNonHiddenSessionIndex()));

        hideThroughTheSessionListHideAction(CURRENT_SESSION_NAME);

        assertEquals("hiding the displayed session must move the terminal view onto a session the owner "
                + "can currently see, and a session inside a project group the owner collapsed renders "
                + "no row at all, so the owner is left looking at a session that is not in the list",
            ON_SCREEN_SESSION_NAME, terminalView.getCurrentSession().mSessionName);
    }

    @Test
    public void hidingTheDisplayedSessionMustNotMoveTheTerminalViewOntoASessionTheSweepAlreadyReleased()
        throws Exception {
        buildSessionList(false);
        collapseThroughTheRealProjectHeaderClick(COLLAPSED_PROJECT_LABEL);

        activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();

        assertNull("test premise: the sweep must have released the dead session inside the collapsed "
                + "group, otherwise this test is not exercising the released-session case at all",
            terminalSessionNamed(COLLAPSED_GROUP_SESSION_NAME).getEmulator());

        hideThroughTheSessionListHideAction(CURRENT_SESSION_NAME);

        assertNotNull("hiding the displayed session must not move the terminal view onto a session the "
                + "sweep already released, because a laid out terminal view starts a fresh shell process "
                + "in it as an invisible side effect of a hide, in a session the owner never chose",
            terminalView.getCurrentSession().getEmulator());
        assertEquals("the terminal view must move onto the session the owner can see that still holds "
                + "its runtime", ON_SCREEN_SESSION_NAME, terminalView.getCurrentSession().mSessionName);
    }

    @Test
    public void hidingTheDisplayedSessionMustPreferAVisibleRowThatStillHoldsItsRuntimeOverAReleasedOne()
        throws Exception {
        buildSessionList(false);
        collapseThroughTheRealProjectHeaderClick(COLLAPSED_PROJECT_LABEL);

        activity.getTermuxTerminalSessionClient().reconnectDeadDefinitionBackedSessionsInBackground();

        collapseThroughTheRealProjectHeaderClick(COLLAPSED_PROJECT_LABEL);

        assertNull("test premise: the released session must be back on screen after the owner expands "
                + "its project group, otherwise this test is not exercising the released-but-visible "
                + "case", terminalSessionNamed(COLLAPSED_GROUP_SESSION_NAME).getEmulator());
        assertEquals("test premise: the released session must be the first row the owner can see, "
                + "otherwise the preference for a row that still holds its runtime is never consulted",
            COLLAPSED_GROUP_SESSION_NAME,
            sessionNameAtIndex(listViewController.getSessionIndexesOfRowsTheOwnerCanSee().get(0)));

        hideThroughTheSessionListHideAction(CURRENT_SESSION_NAME);

        assertEquals("hiding the displayed session must move the terminal view onto a row that still "
                + "holds its runtime rather than the first visible row, because attaching to a released "
                + "row starts a fresh shell process in it as an invisible side effect of the hide",
            ON_SCREEN_SESSION_NAME, terminalView.getCurrentSession().mSessionName);
        assertNotNull("the session the terminal view moved to must still hold its terminal emulator",
            terminalView.getCurrentSession().getEmulator());
    }

    @Test
    public void everyRowTheOwnerCanSeeMustAlsoCountAsDisplayedSoTheSweepCannotReleaseAReplacementCandidate()
        throws Exception {
        buildSessionList(false);
        collapseThroughTheRealProjectHeaderClick(COLLAPSED_PROJECT_LABEL);
        preferences.setSessionDisabled(ON_SCREEN_SESSION_NAME, true);

        Set<String> displayedSessionNames = displayedSessionNamesOfTheClient();
        List<Integer> candidateIndexes = listViewController.getSessionIndexesOfRowsTheOwnerCanSee();

        assertFalse("test premise: at least one row must survive both filters, otherwise the contract "
            + "is asserted over nothing", candidateIndexes.isEmpty());
        for (int candidateIndex : candidateIndexes) {
            String candidateSessionName = sessionNameAtIndex(candidateIndex);
            assertTrue("every row the owner can see must also be counted as displayed, because the "
                    + "reclamation sweep releases a session that is not displayed and the hide moves "
                    + "the terminal view onto exactly these rows: " + candidateSessionName,
                displayedSessionNames.contains(candidateSessionName));
        }
    }

    private Set<String> displayedSessionNamesOfTheClient() throws Exception {
        Method displayedSessionNames = TermuxTerminalSessionActivityClient.class.getDeclaredMethod(
            "displayedSessionNames");
        displayedSessionNames.setAccessible(true);
        return castToSessionNameSet(
            displayedSessionNames.invoke(activity.getTermuxTerminalSessionClient()));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> castToSessionNameSet(Object value) {
        return (Set<String>) value;
    }

    private void buildSessionList(boolean collapsedGroupSessionIsRunning) throws Exception {
        shellManager.mTermuxSessions.add(
            sessionHoldingAnEmulator(COLLAPSED_GROUP_SESSION_NAME, collapsedGroupSessionIsRunning));
        shellManager.mTermuxSessions.add(sessionHoldingAnEmulator(CURRENT_SESSION_NAME, true));
        shellManager.mTermuxSessions.add(sessionHoldingAnEmulator(ON_SCREEN_SESSION_NAME, true));
        terminalView.mTermSession = terminalSessionNamed(CURRENT_SESSION_NAME);

        listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(Arrays.asList(
            new SessionDefinitionEntry(COLLAPSED_PROJECT_LABEL, COLLAPSED_STORY_LABEL,
                Collections.singletonList(COLLAPSED_GROUP_SESSION_NAME)),
            new SessionDefinitionEntry(ON_SCREEN_PROJECT_LABEL, ON_SCREEN_STORY_LABEL,
                Arrays.asList(CURRENT_SESSION_NAME, ON_SCREEN_SESSION_NAME))));
    }

    private void collapseThroughTheRealProjectHeaderClick(String projectLabel) throws Exception {
        Method toggleProjectCollapsed = TermuxSessionsListViewController.class.getDeclaredMethod(
            "toggleProjectCollapsed", String.class);
        toggleProjectCollapsed.setAccessible(true);
        toggleProjectCollapsed.invoke(listViewController, projectLabel);
    }

    private void hideThroughTheSessionListHideAction(String sessionName) throws Exception {
        Method hideSession = TermuxSessionsListViewController.class.getDeclaredMethod(
            "hideSession", String.class);
        hideSession.setAccessible(true);
        hideSession.invoke(listViewController, sessionName);
    }

    private String sessionNameAtIndex(int sessionIndex) {
        TermuxSession termuxSession = service.getTermuxSession(sessionIndex);
        assertNotNull(termuxSession);
        return termuxSession.getTerminalSession().mSessionName;
    }

    private TerminalSession terminalSessionNamed(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull(termuxSession);
        return termuxSession.getTerminalSession();
    }

    private TermuxSession sessionHoldingAnEmulator(String sessionName, boolean running) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession,
            running ? RUNNING_SHELL_PROCESS_PID : TerminalSession.NO_SHELL_PROCESS_PID);
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
