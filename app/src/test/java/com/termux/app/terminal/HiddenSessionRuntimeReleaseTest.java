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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class HiddenSessionRuntimeReleaseTest {

    private static final String CURRENT_SESSION_NAME = "session-current";

    private static final String HIDDEN_SESSION_NAME = "session-alpha";

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;

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

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession currentSession = termuxSession(terminalSession(CURRENT_SESSION_NAME));
        shellManager.mTermuxSessions.add(currentSession);
        terminalView.mTermSession = currentSession.getTerminalSession();
    }

    @Test
    public void hidingASessionReleasesItsTerminalEmulatorWhileItsRowStaysInTheList() throws Exception {
        TermuxSession hiddenSession = sessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(hiddenSession);
        int sessionCountBeforeHiding = service.getTermuxSessionsSize();

        hide(HIDDEN_SESSION_NAME);
        activity.getTermuxTerminalSessionClient().onSessionHiddenStateChanged(HIDDEN_SESSION_NAME, true);

        assertEquals("hiding a session must not remove its row from the session list",
            sessionCountBeforeHiding, service.getTermuxSessionsSize());
        assertNotNull("the row of the hidden session must stay reachable by name so the owner can "
                + "reopen it", service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));
        assertNull("hiding a session must release its terminal emulator and the scrollback buffer the "
                + "emulator owns, so the hidden session holds nothing at runtime",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME).getTerminalSession()
                .getEmulator());
        assertFalse("hiding a session must tear its local shell process down",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME).getTerminalSession()
                .isRunning());
    }

    @Test
    public void unhidingASessionSelectsItForReconnect() throws Exception {
        TermuxSession hiddenSession = sessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        shellManager.mTermuxSessions.add(hiddenSession);
        hide(HIDDEN_SESSION_NAME);
        activity.getTermuxTerminalSessionClient().onSessionHiddenStateChanged(HIDDEN_SESSION_NAME, true);

        hide();
        List<String> reconnectedSessionNames = activity.getTermuxTerminalSessionClient()
            .onSessionHiddenStateChanged(HIDDEN_SESSION_NAME, false);

        assertTrue("unhiding a released session must reconnect it so it resumes normally",
            reconnectedSessionNames.contains(HIDDEN_SESSION_NAME));
    }

    @Test
    public void hidingASessionDoesNotTouchAnyOtherSession() throws Exception {
        TermuxSession hiddenSession = sessionHoldingAnEmulator(HIDDEN_SESSION_NAME);
        TermuxSession displayedSession = sessionHoldingAnEmulator("session-displayed");
        shellManager.mTermuxSessions.add(hiddenSession);
        shellManager.mTermuxSessions.add(displayedSession);

        hide(HIDDEN_SESSION_NAME);
        activity.getTermuxTerminalSessionClient().onSessionHiddenStateChanged(HIDDEN_SESSION_NAME, true);

        assertNotNull("a displayed session must keep its terminal emulator when another session is "
                + "hidden", service.getTermuxSessionForSessionName("session-displayed")
                .getTerminalSession().getEmulator());
    }

    private void hide(String... sessionNames) {
        Set<String> hiddenSessionNames = new LinkedHashSet<>();
        for (String sessionName : sessionNames) {
            hiddenSessionNames.add(sessionName);
        }
        preferences.setDisabledSessionNames(
            TermuxAppSharedPreferences.serializeDisabledSessionNames(hiddenSessionNames));
    }

    private TermuxSession sessionHoldingAnEmulator(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalSession.mSessionName = sessionName;
        Field emulator = TerminalSession.class.getDeclaredField("mEmulator");
        emulator.setAccessible(true);
        emulator.set(terminalSession, new TerminalEmulator(terminalSession, TERMINAL_COLUMNS,
            TERMINAL_ROWS, TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient()));
        return termuxSession(terminalSession);
    }

    private static TerminalSession terminalSession(String sessionName) {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
        return terminalSession;
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
