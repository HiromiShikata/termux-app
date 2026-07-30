package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AlwaysPresentHiddenSessionIsNotRecreatedTest {

    private static final String PROJECT_LABEL = "projectOne";
    private static final String PROJECT_MANAGER_SESSION_NAME =
        PROJECT_LABEL + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    private static final String ALWAYS_NOT_APPLICABLE_SESSION_NAME = "secretary";
    private static final String DISPLAYED_SESSION_NAME = "https://example.test/displayed";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;

    private final List<SessionDefinitionEntry> entries = Collections.singletonList(
        new SessionDefinitionEntry(PROJECT_LABEL, "storyDisplayed",
            Collections.singletonList(DISPLAYED_SESSION_NAME)));

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
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(entries);

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionMaxSessions(16);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        TerminalView terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TermuxSession displayedSession = liveSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        terminalView.mTermSession = displayedSession.getTerminalSession();
    }

    @Test
    public void theAlwaysPresentRestorePlanLeavesOutAHiddenNameSoNothingIsCreatedForIt() {
        preferences.setAlwaysNaSessionNames(ALWAYS_NOT_APPLICABLE_SESSION_NAME);
        recordAsHidden(ALWAYS_NOT_APPLICABLE_SESSION_NAME);

        boolean createdAnything = activity.getTermuxTerminalSessionClient().restoreAlwaysPresentSessions();

        assertFalse("the always-present restore must plan no name at all when the only always-present "
                + "name is one the owner has hidden, and reporting that it created something is the "
                + "signal that the hidden name was planned and started",
            createdAnything);
        assertNull("the always-present restore must not create a session for a name the owner has "
                + "hidden, because the hidden name is required to hold no shell process, no terminal "
                + "emulator and no scrollback until the owner opens it himself",
            service.getTermuxSessionForSessionName(ALWAYS_NOT_APPLICABLE_SESSION_NAME));
    }

    @Test
    public void aDefinitionLoadCreatesNoSessionForAHiddenProjectManagerName() throws Exception {
        recordAsHidden(PROJECT_MANAGER_SESSION_NAME);

        loadTheSessionDefinition();

        assertNull("a definition load must create nothing for the project manager name of a project "
                + "group when the owner has hidden that name; the always-present restore reaches the "
                + "creation call for it and gives the hidden name a live shell again",
            service.getTermuxSessionForSessionName(PROJECT_MANAGER_SESSION_NAME));
        assertEquals("the hidden project manager name must appear nowhere in the live session list "
                + "after a definition load, because the session cap, the reconnect sweep and the "
                + "statusline scan all walk that list by name",
            0, occurrencesInLiveSessionList(PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aDefinitionLoadCreatesNoSessionForAHiddenAlwaysNotApplicableName() throws Exception {
        preferences.setAlwaysNaSessionNames(ALWAYS_NOT_APPLICABLE_SESSION_NAME);
        recordAsHidden(ALWAYS_NOT_APPLICABLE_SESSION_NAME);

        loadTheSessionDefinition();

        assertNull("a definition load must create nothing for a stored always-not-applicable name when "
                + "the owner has hidden it; that stored set is the second way a name enters the "
                + "always-present set and it reaches the same creation call",
            service.getTermuxSessionForSessionName(ALWAYS_NOT_APPLICABLE_SESSION_NAME));
        assertEquals("the hidden always-not-applicable name must appear nowhere in the live session "
                + "list after a definition load",
            0, occurrencesInLiveSessionList(ALWAYS_NOT_APPLICABLE_SESSION_NAME));
    }

    @Test
    public void aDefinitionLoadStillCreatesAnAlwaysPresentSessionThatIsNotHidden() throws Exception {
        preferences.setAlwaysNaSessionNames(ALWAYS_NOT_APPLICABLE_SESSION_NAME);

        loadTheSessionDefinition();

        assertNotNull("keeping hidden names out of the always-present restore must not stop it creating "
                + "the project manager session of a project group that the owner has not hidden",
            service.getTermuxSessionForSessionName(PROJECT_MANAGER_SESSION_NAME));
        assertNotNull("keeping hidden names out of the always-present restore must not stop it creating "
                + "a stored always-not-applicable session that the owner has not hidden",
            service.getTermuxSessionForSessionName(ALWAYS_NOT_APPLICABLE_SESSION_NAME));
    }

    private void loadTheSessionDefinition() throws Exception {
        SessionDefinitionLoadResult authoritativeResult =
            new SessionDefinitionLoadResult(entries, 1, Collections.emptyList());
        SessionDefinitionController controller = new SessionDefinitionController(activity);
        Method buildSessions = SessionDefinitionController.class
            .getDeclaredMethod("buildSessions", SessionDefinitionLoadResult.class);
        buildSessions.setAccessible(true);
        buildSessions.invoke(controller, authoritativeResult);
    }

    private void recordAsHidden(String sessionName) {
        preferences.setDisabledSessionNames(TermuxAppSharedPreferences.serializeDisabledSessionNames(
            Collections.singleton(sessionName)));
    }

    private int occurrencesInLiveSessionList(String sessionName) {
        int occurrences = 0;
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession != null && sessionName.equals(terminalSession.mSessionName)) occurrences++;
        }
        return occurrences;
    }

    private TermuxSession liveSession(String sessionName) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = sessionName;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, 4242);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
