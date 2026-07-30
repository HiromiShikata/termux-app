package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionLoadResult;
import com.termux.app.sessiondefinition.SessionDefinitionRepository;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.session.PersistedSessionRestoreData;
import com.termux.app.terminal.session.PersistedSessionSerializer;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.view.TerminalView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ColdStartSessionCreationTest {

    private static final String ALWAYS_PRESENT_SESSION_NAME = "secretary";
    private static final String HIDDEN_ALWAYS_PRESENT_SESSION_NAME = "tdpmcli";
    private static final String PERSISTED_SESSION_NAME = "persisted-agent";
    private static final String FIRST_PROJECT_LABEL = "alpha";
    private static final String SECOND_PROJECT_LABEL = "beta";
    private static final String FIRST_PROJECT_MANAGER_SESSION_NAME = "alphapm";
    private static final String SECOND_PROJECT_MANAGER_SESSION_NAME = "betapm";
    private static final int SESSION_CAP = 16;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxAppSharedPreferences preferences;
    private BlankSessionRecordingSessionClient sessionClient;
    private final PersistedSessionSerializer persistedSessionSerializer = new PersistedSessionSerializer();

    private static final class BlankSessionRecordingSessionClient extends TermuxTerminalSessionActivityClient {

        private int blankSessionCreationCallCount;

        private BlankSessionRecordingSessionClient(TermuxActivity activity) {
            super(activity);
        }

        @Override
        public void addNewSession(boolean isFailSafe, String sessionName) {
            blankSessionCreationCallCount++;
        }
    }

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        sessionClient = new BlankSessionRecordingSessionClient(activity);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", sessionClient);
        service.setTermuxTerminalSessionClient(sessionClient);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionMaxSessions(SESSION_CAP);

        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(Collections.emptyList());

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        set(activity, TermuxActivity.class, "mTerminalView", new TerminalView(appContext, null));
    }

    @Test
    public void anAlwaysPresentSessionMissingFromThePersistedListIsCreatedEvenWhenThePersistedRestoreSucceeded()
            throws Exception {
        preferences.setAlwaysNaSessionNames(ALWAYS_PRESENT_SESSION_NAME);
        persistOneSessionNamed(PERSISTED_SESSION_NAME);

        createStartupSessions();

        assertNotNull("restoring the persisted session list must not stop the always-present session from "
                + "being created, because the owner is required to get every always-present session back on "
                + "a cold start without creating any of them by hand; the live session names were "
                + liveSessionNames(), service.getTermuxSessionForSessionName(ALWAYS_PRESENT_SESSION_NAME));
        assertNotNull("the persisted session must still be restored; the live session names were "
            + liveSessionNames(), service.getTermuxSessionForSessionName(PERSISTED_SESSION_NAME));
        assertEquals("a blank session must not be added when a session was already restored or created; the "
                + "live session names were " + liveSessionNames(),
            0, sessionClient.blankSessionCreationCallCount);
    }

    @Test
    public void aProjectManagerSessionIsCreatedForEveryProjectWithoutAnyLoadSessionsAction() throws Exception {
        cacheSessionDefinitionEntriesForProjects(FIRST_PROJECT_LABEL, SECOND_PROJECT_LABEL);

        createStartupSessions();

        assertNotNull("every project's project-manager session must be created on a cold start without the "
                + "owner pressing Load Sessions; the live session names were " + liveSessionNames(),
            service.getTermuxSessionForSessionName(FIRST_PROJECT_MANAGER_SESSION_NAME));
        assertNotNull("every project's project-manager session must be created on a cold start without the "
                + "owner pressing Load Sessions; the live session names were " + liveSessionNames(),
            service.getTermuxSessionForSessionName(SECOND_PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aHiddenAlwaysPresentSessionStaysUncreatedOnAColdStart() throws Exception {
        preferences.setAlwaysNaSessionNames(
            ALWAYS_PRESENT_SESSION_NAME + "\n" + HIDDEN_ALWAYS_PRESENT_SESSION_NAME);
        preferences.setDisabledSessionNames(TermuxAppSharedPreferences.serializeDisabledSessionNames(
            new LinkedHashSet<>(Collections.singletonList(HIDDEN_ALWAYS_PRESENT_SESSION_NAME))));

        createStartupSessions();

        assertTrue("the arrangement must record the name as hidden before the assertion below means "
                + "anything; the stored hidden set was " + preferences.getDisabledSessionNames(),
            preferences.getDisabledSessionNames().contains(HIDDEN_ALWAYS_PRESENT_SESSION_NAME));
        assertNull("a name the owner explicitly hid must stay uncreated on a cold start; the live session "
                + "names were " + liveSessionNames(),
            service.getTermuxSessionForSessionName(HIDDEN_ALWAYS_PRESENT_SESSION_NAME));
        assertNotNull("the always-present name the owner did not hide must still be created; the live "
                + "session names were " + liveSessionNames(),
            service.getTermuxSessionForSessionName(ALWAYS_PRESENT_SESSION_NAME));
    }

    private void createStartupSessions() throws Exception {
        Method createStartupSessions =
            TermuxActivity.class.getDeclaredMethod("createStartupSessions", boolean.class);
        createStartupSessions.setAccessible(true);
        createStartupSessions.invoke(activity, false);
    }

    private void persistOneSessionNamed(String sessionName) throws Exception {
        preferences.setPersistedSessions(persistedSessionSerializer.serialize(Collections.singletonList(
            new PersistedSessionRestoreData(null, sessionName, "/system/bin/sh", new String[0], false, "/"))));
    }

    private void cacheSessionDefinitionEntriesForProjects(String... projectLabels) throws Exception {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        for (String projectLabel : projectLabels) {
            entries.add(new SessionDefinitionEntry(projectLabel, projectLabel + "-story",
                Collections.singletonList("https://example.test/" + projectLabel)));
        }
        SessionDefinitionRepository repository = (SessionDefinitionRepository)
            read(activity, TermuxActivity.class, "mSessionDefinitionRepository");
        set(repository, SessionDefinitionRepository.class, "result",
            new SessionDefinitionLoadResult(entries, projectLabels.length, Collections.emptyList()));
        set(repository, SessionDefinitionRepository.class, "loaded", true);
    }

    private List<String> liveSessionNames() {
        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }
        return liveSessionNames;
    }

    private Object read(Object target, Class<?> declaringClass, String fieldName) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
