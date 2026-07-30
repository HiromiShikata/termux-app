package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.OpenedSessionUnhider;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OwnerHiddenMarkSurvivesTheDefinitionDocumentTest {

    private static final String DISPLAYED_SESSION_NAME = "session-displayed";
    private static final String LISTED_GITHUB_SESSION_NAME = "https://github.com/owner/repo/issues/1";
    private static final String HIDDEN_GITHUB_SESSION_NAME = "https://github.com/owner/repo/issues/2";
    private static final String DISAPPEARED_GITHUB_SESSION_NAME = "https://github.com/owner/repo/issues/3";

    private final List<SessionDefinitionEntry> entriesWithoutTheHiddenSession = Collections.singletonList(
        new SessionDefinitionEntry("projectOne", "storyListed",
            Collections.singletonList(LISTED_GITHUB_SESSION_NAME)));

    private final List<SessionDefinitionEntry> entriesWithTheHiddenSession = Arrays.asList(
        new SessionDefinitionEntry("projectOne", "storyListed",
            Collections.singletonList(LISTED_GITHUB_SESSION_NAME)),
        new SessionDefinitionEntry("projectOne", "storyHidden",
            Collections.singletonList(HIDDEN_GITHUB_SESSION_NAME)));

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
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(entriesWithoutTheHiddenSession);

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionMaxSessions(16);
        preferences.setRemoveGithubSessionsNotInList(true);

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
    public void anAuthoritativeLoadWhoseEntriesOmitAHiddenSessionKeepsThatSessionHidden() throws Exception {
        recordAsHidden(HIDDEN_GITHUB_SESSION_NAME);

        loadTheSessionDefinition(entriesWithoutTheHiddenSession);

        assertTrue("the hidden mark belongs to the owner and is his alone to clear, so an authoritative "
                + "load whose fetched entries no longer mention the name must leave the mark exactly as "
                + "the owner left it",
            preferences.getDisabledSessionNames().contains(HIDDEN_GITHUB_SESSION_NAME));
    }

    @Test
    public void aNameThatLeftTheDefinitionDocumentAndReturnedIsStillNotCreated() throws Exception {
        recordAsHidden(HIDDEN_GITHUB_SESSION_NAME);

        loadTheSessionDefinition(entriesWithoutTheHiddenSession);
        loadTheSessionDefinition(entriesWithTheHiddenSession);

        assertNull("the fetched definition document is refetched in full on every load and holds no "
                + "record of what it used to contain, so a name can leave it and return; when it "
                + "returns the owner has still not opened the session and no session may be created "
                + "for it",
            service.getTermuxSessionForSessionName(HIDDEN_GITHUB_SESSION_NAME));
        assertTrue("a round trip out of the fetched definition document and back into it must leave the "
                + "owner's hidden mark untouched",
            preferences.getDisabledSessionNames().contains(HIDDEN_GITHUB_SESSION_NAME));
    }

    @Test
    public void aGithubSessionThatIsNoLongerInTheDefinitionListIsStillRemoved() throws Exception {
        shellManager.mTermuxSessions.add(liveSession(DISAPPEARED_GITHUB_SESSION_NAME));

        loadTheSessionDefinition(entriesWithoutTheHiddenSession);

        assertNull("removing a live session whose GitHub name is no longer in the fetched session list "
                + "is intended behaviour and must keep working; only the erasure of the owner's hidden "
                + "mark is the defect",
            service.getTermuxSessionForSessionName(DISAPPEARED_GITHUB_SESSION_NAME));
        assertNotNull("removing the sessions that left the fetched session list must not remove the "
                + "session the owner is looking at",
            service.getTermuxSessionForSessionName(DISPLAYED_SESSION_NAME));
    }

    @Test
    public void theOwnerUnhideActionStillClearsTheHiddenMark() {
        recordAsHidden(HIDDEN_GITHUB_SESSION_NAME);

        boolean unhid = OpenedSessionUnhider.unhideOpenedSession(
            preferences, HIDDEN_GITHUB_SESSION_NAME);

        assertTrue("navigating to a hidden session is the owner asking for it back, so it must still "
                + "report that it cleared the mark",
            unhid);
        assertFalse("the owner's own unhide action must still clear the hidden mark; keeping the mark "
                + "against the definition document must not freeze the mark against the owner",
            preferences.getDisabledSessionNames().contains(HIDDEN_GITHUB_SESSION_NAME));
    }

    private void loadTheSessionDefinition(List<SessionDefinitionEntry> entries) throws Exception {
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
