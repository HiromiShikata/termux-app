package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

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
public class HiddenSessionIsNotRecreatedByADefinitionReloadTest {

    private static final String DISPLAYED_SESSION_NAME = "https://example.test/displayed";
    private static final String HIDDEN_SESSION_NAME = "https://example.test/hidden";
    private static final String ABSENT_SESSION_NAME = "https://example.test/absent";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;

    private final List<SessionDefinitionEntry> entries = Arrays.asList(
        new SessionDefinitionEntry("projectOne", "storyDisplayed",
            Collections.singletonList(DISPLAYED_SESSION_NAME)),
        new SessionDefinitionEntry("projectOne", "storyHidden",
            Collections.singletonList(HIDDEN_SESSION_NAME)),
        new SessionDefinitionEntry("projectOne", "storyAbsent",
            Collections.singletonList(ABSENT_SESSION_NAME)));

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

        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(entries);

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("");
    }

    @Test
    public void aDefinitionReloadDoesNotRecreateASessionTheOwnerHasHidden() throws Exception {
        TermuxSession displayedSession = liveSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        attachCurrentSession(displayedSession.getTerminalSession());
        recordAsHidden(HIDDEN_SESSION_NAME);

        assertNull("the arrangement must start with the hidden session holding no live session object, "
                + "because that is what hiding leaves behind and it is exactly what makes the reload "
                + "treat the name as one that needs creating",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));

        reloadTheSessionDefinition();

        assertNull("a definition reload must not recreate a session the owner has hidden; recreating it "
                + "gives the hidden name a shell process, a terminal emulator and a scrollback buffer "
                + "again, which defeats the whole requirement that a hidden session holds nothing",
            service.getTermuxSessionForSessionName(HIDDEN_SESSION_NAME));
        assertSame("a definition reload must leave the owner on the session he is working in, so a "
                + "hidden session must not be recreated and then displayed",
            displayedSession.getTerminalSession(), activity.getCurrentSession());
        assertTrue("the hide must survive the reload in stored state as well, otherwise the row would "
                + "come back shown on the next refresh",
            preferences.getDisabledSessionNames().contains(HIDDEN_SESSION_NAME));
    }

    @Test
    public void aDefinitionReloadStillRecreatesADefinedSessionThatIsAbsentAndNotHidden() throws Exception {
        TermuxSession displayedSession = liveSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        attachCurrentSession(displayedSession.getTerminalSession());
        recordAsHidden(HIDDEN_SESSION_NAME);

        assertNull("the arrangement must start with the absent session holding no live session object, "
                + "otherwise this test cannot show that the reload creates one",
            service.getTermuxSessionForSessionName(ABSENT_SESSION_NAME));

        reloadTheSessionDefinition();

        assertNotNull("keeping hidden sessions out of the reload must not stop the reload creating a "
                + "defined session that is genuinely absent and not hidden; that is the reload's whole "
                + "purpose and the guard must not be implemented by refusing to create anything",
            service.getTermuxSessionForSessionName(ABSENT_SESSION_NAME));
    }

    @Test
    public void aDefinitionReloadLeavesAHiddenSessionOutOfTheLiveSessionListEntirely() throws Exception {
        TermuxSession displayedSession = liveSession(DISPLAYED_SESSION_NAME);
        shellManager.mTermuxSessions.add(displayedSession);
        attachCurrentSession(displayedSession.getTerminalSession());
        recordAsHidden(HIDDEN_SESSION_NAME);

        reloadTheSessionDefinition();

        assertEquals("a hidden name must appear nowhere in the live session list after a reload, because "
                + "every selection that reads that list — the session cap, the reconnect scheduler and "
                + "the statusline re-parse set — walks it by name",
            0, occurrencesInLiveSessionList(HIDDEN_SESSION_NAME));
    }

    private void reloadTheSessionDefinition() throws Exception {
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

    private void attachCurrentSession(TerminalSession terminalSession) throws Exception {
        com.termux.view.TerminalView terminalView =
            new com.termux.view.TerminalView(RuntimeEnvironment.getApplication(), null);
        terminalView.mTermSession = terminalSession;
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
