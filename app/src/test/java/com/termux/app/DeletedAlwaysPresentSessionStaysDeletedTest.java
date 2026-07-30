package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A session the owner deleted must stay deleted. An always-present session name was exempted from the
 * removal record, so the always-present restore created that name again on the next sweep and the
 * deleted session came back. Deleting a session must never edit the always-present name list the owner
 * typed in the settings screen, so the removal is recorded per session instead.
 */
@RunWith(RobolectricTestRunner.class)
public class DeletedAlwaysPresentSessionStaysDeletedTest {

    private static final String ALWAYS_PRESENT_SESSION_NAME = "secretary";
    private static final String SECOND_ALWAYS_PRESENT_SESSION_NAME = "tdpmcli";
    private static final int SESSION_CAP = 16;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxAppSharedPreferences preferences;
    private TermuxTerminalSessionActivityClient sessionClient;

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(appContext));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        sessionClient = new TermuxTerminalSessionActivityClient(activity);
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

    private List<String> liveSessionNames() {
        List<String> names = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            names.add(termuxSession.getTerminalSession().mSessionName);
        }
        return names;
    }

    private TermuxSession createThenDeleteTheAlwaysPresentSession() {
        preferences.setAlwaysNaSessionNames(
            ALWAYS_PRESENT_SESSION_NAME + "\n" + SECOND_ALWAYS_PRESENT_SESSION_NAME);
        sessionClient.restoreAlwaysPresentSessions();
        TermuxSession created = service.getTermuxSessionForSessionName(ALWAYS_PRESENT_SESSION_NAME);
        assertNotNull("the always-present session must be created before it can be deleted; the live "
            + "session names were " + liveSessionNames(), created);
        sessionClient.deleteSession(created.getTerminalSession());
        return created;
    }

    @Test
    public void deletingAnAlwaysPresentSessionRecordsThatTheOwnerRemovedIt() {
        createThenDeleteTheAlwaysPresentSession();

        assertTrue("deleting an always-present session must record the removal for that session name, "
                + "because the owner expects a deletion to be a deletion",
            preferences.isSessionUserRemoved(ALWAYS_PRESENT_SESSION_NAME));
    }

    @Test
    public void anAlwaysPresentSessionTheOwnerDeletedIsNotCreatedAgainByTheAlwaysPresentRestore() {
        createThenDeleteTheAlwaysPresentSession();

        sessionClient.restoreAlwaysPresentSessions();

        assertNull("an always-present session the owner deleted must stay deleted instead of being "
                + "created again by the next always-present restore; the live session names were "
                + liveSessionNames(),
            service.getTermuxSessionForSessionName(ALWAYS_PRESENT_SESSION_NAME));
    }

    @Test
    public void deletingOneAlwaysPresentSessionLeavesEveryOtherAlwaysPresentSessionAlone() {
        createThenDeleteTheAlwaysPresentSession();

        sessionClient.restoreAlwaysPresentSessions();

        assertNotNull("deleting one always-present session must not stop any other always-present "
                + "session from being created; the live session names were " + liveSessionNames(),
            service.getTermuxSessionForSessionName(SECOND_ALWAYS_PRESENT_SESSION_NAME));
        assertFalse("no other always-present session name may be recorded as removed",
            preferences.isSessionUserRemoved(SECOND_ALWAYS_PRESENT_SESSION_NAME));
    }

    @Test
    public void deletingAnAlwaysPresentSessionNeverEditsTheAlwaysPresentNameListTheOwnerTyped() {
        String typedNameList = ALWAYS_PRESENT_SESSION_NAME + "\n" + SECOND_ALWAYS_PRESENT_SESSION_NAME;

        createThenDeleteTheAlwaysPresentSession();

        assertTrue("the always-present name list the owner typed must still carry the deleted name, "
                + "because a deletion must never edit the owner's own configuration; the list held "
                + preferences.getAlwaysNaSessionNames(),
            preferences.getAlwaysNaSessionNames().contains(ALWAYS_PRESENT_SESSION_NAME));
        assertTrue("the always-present name list the owner typed must be left exactly as it was; the "
                + "list held " + preferences.getAlwaysNaSessionNames(),
            preferences.getAlwaysNaSessionNames().contains(SECOND_ALWAYS_PRESENT_SESSION_NAME)
                && typedNameList.contains(SECOND_ALWAYS_PRESENT_SESSION_NAME));
    }

    @Test
    public void anAlwaysPresentSessionNobodyDeletedIsStillCreatedByTheAlwaysPresentRestore() {
        preferences.setAlwaysNaSessionNames(ALWAYS_PRESENT_SESSION_NAME);

        sessionClient.restoreAlwaysPresentSessions();

        assertNotNull("an always-present session nobody deleted must still be created automatically, "
                + "because the owner is required to get every always-present session back without "
                + "creating any of them by hand; the live session names were " + liveSessionNames(),
            service.getTermuxSessionForSessionName(ALWAYS_PRESENT_SESSION_NAME));
    }
}
