package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.terminal.session.PersistedSessionRestoreData;
import com.termux.app.terminal.session.PersistedSessionSerializer;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class StartupDisplayedSessionColdStartOrderingTest {

    private static final String ALPHA_PROJECT = "alphaProject";
    private static final String BETA_PROJECT = "betaProject";

    private static final String ALPHA_ONE = "https://example.test/alpha-one";
    private static final String ALPHA_TWO = "https://example.test/alpha-two";
    private static final String BETA_ONE = "https://example.test/beta-one";

    private static final List<SessionDefinitionEntry> ENTRIES = Arrays.asList(
        new SessionDefinitionEntry(ALPHA_PROJECT, "alphaStory", Arrays.asList(ALPHA_ONE, ALPHA_TWO)),
        new SessionDefinitionEntry(BETA_PROJECT, "betaStory", Collections.singletonList(BETA_ONE)));

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
        preferences.setSessionDefinitionMaxSessions(10);

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
    }

    @Test
    public void coldStartSelectionIsCorrectedOnceTheSessionDefinitionEntriesArrive() throws Exception {
        addSessions(BETA_ONE, ALPHA_ONE, ALPHA_TWO);
        attachSessionListWithEntries(Collections.emptyList());

        activity.getTermuxTerminalSessionClient().setCurrentSessionOnReconnectIfNoneDisplayed();
        assertEquals("with no entries loaded the row builder can only offer plain service order",
            BETA_ONE, currentSessionName());

        activity.refreshDisplayedSessionDefinitionEntries(ENTRIES);

        assertEquals("once the entries arrive the grouped display order puts a different session on top, "
                + "and the startup selection must be re-applied against it",
            ALPHA_ONE, currentSessionName());
    }

    @Test
    public void aSessionTheUserSelectedBeforeTheEntriesArriveIsNotTakenAway() throws Exception {
        addSessions(BETA_ONE, ALPHA_ONE, ALPHA_TWO);
        attachSessionListWithEntries(Collections.emptyList());

        activity.getTermuxTerminalSessionClient().setCurrentSessionOnReconnectIfNoneDisplayed();
        activity.getTermuxTerminalSessionClient().setCurrentSession(terminalSession(ALPHA_TWO));

        activity.refreshDisplayedSessionDefinitionEntries(ENTRIES);

        assertEquals("an explicit selection must survive the late arrival of the session definition entries",
            ALPHA_TWO, currentSessionName());
    }

    @Test
    public void restoredSessionSelectionSkipsTheHiddenSessionThatWasRestoredFirst() throws Exception {
        attachSessionListWithEntries(ENTRIES);
        preferences.setDisabledSessionNames(ALPHA_ONE);
        preferences.setPersistedSessions(new PersistedSessionSerializer().serialize(Arrays.asList(
            new PersistedSessionRestoreData(null, ALPHA_ONE, "/system/bin/sh", new String[0], false, "/"),
            new PersistedSessionRestoreData(null, ALPHA_TWO, "/system/bin/sh", new String[0], false, "/"))));

        assertTrue(activity.getTermuxTerminalSessionClient().restorePersistedSessions());

        assertEquals("restore must not display the hidden session merely because it was restored first",
            ALPHA_TWO, currentSessionName());
    }

    @Test
    public void alwaysPresentSessionSelectionSkipsTheHiddenSessionThatWasCreatedFirst() throws Exception {
        attachSessionListWithEntries(ENTRIES);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setDisabledSessionNames(ALPHA_ONE);

        assertTrue(activity.getTermuxTerminalSessionClient()
            .restoreAlwaysPresentSessions(Arrays.asList(ALPHA_ONE, ALPHA_TWO)));

        assertEquals("always-present creation must not display the hidden session merely because it was "
                + "created first",
            ALPHA_TWO, currentSessionName());
    }

    @Test
    public void openingASessionAttachesItToTheTerminalViewWhetherOrNotItIsHidden() throws Exception {
        addSessions(ALPHA_ONE, ALPHA_TWO);
        attachSessionListWithEntries(ENTRIES);
        preferences.setDisabledSessionNames(ALPHA_ONE);

        activity.getTermuxTerminalSessionClient().setCurrentSession(terminalSession(ALPHA_TWO));
        String attachedNonHiddenSessionName = terminalView.getCurrentSession().mSessionName;

        activity.getTermuxTerminalSessionClient().setCurrentSession(terminalSession(ALPHA_ONE));
        String attachedHiddenSessionName = terminalView.getCurrentSession().mSessionName;

        assertEquals(ALPHA_TWO, attachedNonHiddenSessionName);
        assertEquals("the hidden set must gate only the automatic startup pass, never the path that runs "
                + "when the user opens a session",
            ALPHA_ONE, attachedHiddenSessionName);
    }

    private void attachSessionListWithEntries(List<SessionDefinitionEntry> entries) throws Exception {
        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(entries);
    }

    private void addSessions(String... sessionNames) throws Exception {
        for (String sessionName : sessionNames) {
            shellManager.mTermuxSessions.add(liveSession(sessionName));
        }
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

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
