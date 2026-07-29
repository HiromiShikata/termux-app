package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class StartupDisplayedSessionSelectionTest {

    private static final String ALPHA_PROJECT = "alphaProject";
    private static final String BETA_PROJECT = "betaProject";

    private static final String ALPHA_ONE = "https://example.test/alpha-one";
    private static final String ALPHA_TWO = "https://example.test/alpha-two";
    private static final String BETA_ONE = "https://example.test/beta-one";
    private static final String BETA_TWO = "https://example.test/beta-two";

    private static final List<SessionDefinitionEntry> ENTRIES = Arrays.asList(
        new SessionDefinitionEntry(ALPHA_PROJECT, "alphaStory", Arrays.asList(ALPHA_ONE, ALPHA_TWO)),
        new SessionDefinitionEntry(BETA_PROJECT, "betaStory", Arrays.asList(BETA_ONE, BETA_TWO)));

    private static final List<String> SERVICE_ORDER =
        Arrays.asList(BETA_ONE, ALPHA_ONE, ALPHA_TWO, BETA_TWO);

    private static final List<String> DISPLAY_ORDER =
        Arrays.asList(ALPHA_ONE, ALPHA_TWO, BETA_ONE, BETA_TWO);

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private TerminalView terminalView;

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
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        for (String sessionName : SERVICE_ORDER) {
            shellManager.mTermuxSessions.add(liveSession(sessionName));
        }
    }

    @Test
    public void startupDisplaysTheTopmostNonHiddenSessionWhenTheTopmostSessionInDisplayOrderIsHidden() {
        buildSessionListWithCollapsedProjects(Collections.emptySet());
        assertEquals("the fixture must order the rows by project and story, not by service insertion order",
            DISPLAY_ORDER, orderedDisplaySessionNames());
        hideSessions(ALPHA_ONE);

        selectStartupSession();

        assertEquals("startup must display the topmost session that is not hidden",
            ALPHA_TWO, currentSessionName());
    }

    @Test
    public void startupSelectsTheSameSessionWithTheHideHiddenSessionsToggleOnAndOff() {
        buildSessionListWithCollapsedProjects(Collections.emptySet());
        hideSessions(ALPHA_ONE);

        preferences.setHideHiddenSessions(false);
        selectStartupSession();
        String selectedWithToggleOff = currentSessionName();

        clearDisplayedSession();
        preferences.setHideHiddenSessions(true);
        selectStartupSession();
        String selectedWithToggleOn = currentSessionName();

        assertEquals("the hide_hidden_sessions toggle only controls rendering and must not change the selection",
            selectedWithToggleOff, selectedWithToggleOn);
        assertEquals("startup must display the topmost session that is not hidden regardless of the toggle",
            ALPHA_TWO, selectedWithToggleOn);
    }

    @Test
    public void startupSelectsANonHiddenSessionThatSitsInsideACollapsedProjectGroup() {
        buildSessionListWithCollapsedProjects(Collections.singleton(BETA_PROJECT));
        hideSessions(ALPHA_ONE, ALPHA_TWO);

        selectStartupSession();

        assertEquals("a collapsed project group hides rows from the list but must not make its sessions "
                + "ineligible for the startup selection",
            BETA_ONE, currentSessionName());
    }

    @Test
    public void startupKeepsThePreviousFallbackSelectionWhenEverySessionIsHidden() {
        buildSessionListWithCollapsedProjects(Collections.emptySet());
        hideSessions(ALPHA_ONE, ALPHA_TWO, BETA_ONE, BETA_TWO);

        selectStartupSession();

        assertEquals("with no non-hidden session to select, the previous fallback must still display a session",
            BETA_TWO, currentSessionName());
    }

    @Test
    public void startupDoesNotSwitchAwayFromASessionThatIsAlreadyValidlyDisplayed() {
        buildSessionListWithCollapsedProjects(Collections.emptySet());
        hideSessions(ALPHA_ONE);
        terminalView.mTermSession = terminalSession(ALPHA_ONE);

        selectStartupSession();

        assertEquals("the reconnect guard must keep the already displayed session even when it is hidden",
            ALPHA_ONE, currentSessionName());
    }

    private void selectStartupSession() {
        activity.getTermuxTerminalSessionClient().setCurrentSessionOnReconnectIfNoneDisplayed();
    }

    private void clearDisplayedSession() {
        terminalView.mTermSession = null;
    }

    private void buildSessionListWithCollapsedProjects(Set<String> collapsedProjectLabels) {
        preferences.setCollapsedProjectKeys(new LinkedHashSet<>(collapsedProjectLabels));
        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        setQuietly(activity, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(ENTRIES);
    }

    private void hideSessions(String... sessionNames) {
        preferences.setDisabledSessionNames(String.join("\n", sessionNames));
    }

    private List<String> orderedDisplaySessionNames() {
        TermuxSessionsListViewController listViewController =
            activity.getTermuxSessionListViewController();
        assertNotNull(listViewController);
        List<String> sessionNamesByIndex = listViewController.getSessionNamesByIndex();
        List<String> orderedSessionNames = new ArrayList<>();
        for (int sessionIndex : listViewController.getOrderedSessionIndexes()) {
            orderedSessionNames.add(sessionNamesByIndex.get(sessionIndex));
        }
        return orderedSessionNames;
    }

    private String currentSessionName() {
        TerminalSession currentSession = activity.getCurrentSession();
        assertNotNull("a session must be displayed after the startup selection runs", currentSession);
        return currentSession.mSessionName;
    }

    private TerminalSession terminalSession(String sessionName) {
        TermuxSession termuxSession = service.getTermuxSessionForSessionName(sessionName);
        assertNotNull(termuxSession);
        return termuxSession.getTerminalSession();
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void setQuietly(Object target, String fieldName, Object value) {
        try {
            set(target, TermuxActivity.class, fieldName, value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
