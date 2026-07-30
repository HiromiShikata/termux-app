package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.SessionShortcut;
import com.termux.app.terminal.SessionShortcutBarPlanner;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.session.PersistedSession;
import com.termux.app.terminal.session.PersistedSessionRestoreData;
import com.termux.app.terminal.session.PersistedSessionSerializer;
import com.termux.shared.shell.command.ExecutionCommand;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionEmptyLoadRetentionTest {

    private static final String FIRST_GITHUB_SESSION_NAME = "https://github.com/owner/repo/issues/1";
    private static final String SECOND_GITHUB_SESSION_NAME = "https://github.com/owner/repo/issues/2";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxSessionsListViewController adapter;

    private final List<SessionDefinitionEntry> definedEntries = Arrays.asList(
        new SessionDefinitionEntry("projectOne", "storyA",
            Collections.singletonList(FIRST_GITHUB_SESSION_NAME)),
        new SessionDefinitionEntry("projectTwo", "storyB",
            Collections.singletonList(SECOND_GITHUB_SESSION_NAME)));

    private final SessionDefinitionLoadResult emptyButParseableDocumentResult =
        new SessionDefinitionLoadResult(Collections.emptyList(), 2, Collections.emptyList());

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

        adapter = new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", adapter);
        adapter.setEntries(definedEntries);

        set(activity, TermuxActivity.class, "mPreferences",
            com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences.build(appContext, true));
        activity.getPreferences().setRemoveGithubSessionsNotInList(true);
    }

    @Test
    public void emptyButParseableDocumentKeepsLiveGithubNamedSessions() throws Exception {
        TermuxSession firstSession = session(FIRST_GITHUB_SESSION_NAME);
        TermuxSession secondSession = session(SECOND_GITHUB_SESSION_NAME);
        shellManager.mTermuxSessions.add(firstSession);
        shellManager.mTermuxSessions.add(secondSession);
        attachCurrentSession(firstSession.getTerminalSession());

        invokeBuildSessions(emptyButParseableDocumentResult);

        assertEquals(Arrays.asList(FIRST_GITHUB_SESSION_NAME, SECOND_GITHUB_SESSION_NAME),
            remainingSessionNames());
    }

    @Test
    public void emptyButParseableDocumentKeepsPersistedSessionRecords() throws Exception {
        TermuxSession firstSession = session(FIRST_GITHUB_SESSION_NAME);
        TermuxSession secondSession = session(SECOND_GITHUB_SESSION_NAME);
        shellManager.mTermuxSessions.add(firstSession);
        shellManager.mTermuxSessions.add(secondSession);
        attachCurrentSession(firstSession.getTerminalSession());
        persistSession(firstSession.getTerminalSession());
        persistSession(secondSession.getTerminalSession());
        assertEquals(Arrays.asList(FIRST_GITHUB_SESSION_NAME, SECOND_GITHUB_SESSION_NAME),
            persistedSessionNames());

        invokeBuildSessions(emptyButParseableDocumentResult);

        assertEquals(Arrays.asList(FIRST_GITHUB_SESSION_NAME, SECOND_GITHUB_SESSION_NAME),
            persistedSessionNames());
    }

    @Test
    public void emptyButParseableDocumentKeepsDisplayedDefinitionRows() throws Exception {
        TermuxSession firstSession = session(FIRST_GITHUB_SESSION_NAME);
        shellManager.mTermuxSessions.add(firstSession);
        attachCurrentSession(firstSession.getTerminalSession());

        invokeBuildSessions(emptyButParseableDocumentResult);

        assertEquals(Arrays.asList("storyA", "storyB"), entryLabels(adapter.getEntries()));
    }

    @Test
    public void emptyButParseableDocumentKeepsProjectManagerShortcuts() throws Exception {
        TermuxSession firstSession = session(FIRST_GITHUB_SESSION_NAME);
        shellManager.mTermuxSessions.add(firstSession);
        attachCurrentSession(firstSession.getTerminalSession());

        invokeBuildSessions(emptyButParseableDocumentResult);

        SessionShortcutBarPlanner shortcutBarPlanner =
            new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());
        List<SessionShortcut> shortcuts = shortcutBarPlanner.planRightToLeftShortcuts(
            Collections.emptySet(), adapter.getEntries());

        assertEquals(Arrays.asList(
            new SessionShortcut("projectOne",
                "projectOne" + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX),
            new SessionShortcut("projectTwo",
                "projectTwo" + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX)),
            shortcuts);
    }

    private void invokeBuildSessions(SessionDefinitionLoadResult result) throws Exception {
        SessionDefinitionController controller = new SessionDefinitionController(activity);
        Method buildMethod = SessionDefinitionController.class
            .getDeclaredMethod("buildSessions", SessionDefinitionLoadResult.class);
        buildMethod.setAccessible(true);
        buildMethod.invoke(controller, result);
    }

    private void persistSession(TerminalSession terminalSession) throws Exception {
        TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
        Field storeField = TermuxTerminalSessionActivityClient.class
            .getDeclaredField("mPersistedSessionBySession");
        storeField.setAccessible(true);
        Map.class.getMethod("put", Object.class, Object.class).invoke(storeField.get(client), terminalSession,
            new PersistedSession(terminalSession.mHandle, "/bin/sh", new String[0], false, "/"));
        Method saveMethod = TermuxTerminalSessionActivityClient.class
            .getDeclaredMethod("savePersistedSessions");
        saveMethod.setAccessible(true);
        saveMethod.invoke(client);
    }

    private List<String> persistedSessionNames() throws Exception {
        List<String> names = new ArrayList<>();
        for (PersistedSessionRestoreData restoreData : new PersistedSessionSerializer()
                .deserialize(activity.getPreferences().getPersistedSessions()))
            names.add(restoreData.getName());
        return names;
    }

    private List<String> remainingSessionNames() {
        List<String> names = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions())
            names.add(termuxSession.getTerminalSession().mSessionName);
        return names;
    }

    private List<String> entryLabels(List<SessionDefinitionEntry> entriesToRead) {
        List<String> labels = new ArrayList<>();
        for (SessionDefinitionEntry entry : entriesToRead)
            labels.add(entry.getEntryLabel());
        return labels;
    }

    private TermuxSession session(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class, boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
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
