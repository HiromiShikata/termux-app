package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.UserRemovedSessionHideWindow;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
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

@RunWith(RobolectricTestRunner.class)
public class OwnerDeletedSessionStaysOutOfTheListForFifteenMinutesTest {

    private static final String DELETED_SESSION_NAME = "https://example.test/deleted-by-the-owner";

    private static final String UNTOUCHED_SESSION_NAME = "https://example.test/left-alone";

    private final List<SessionDefinitionEntry> publishedEntries = Collections.singletonList(
        new SessionDefinitionEntry("projectOne", "storyA",
            Arrays.asList(DELETED_SESSION_NAME, UNTOUCHED_SESSION_NAME)));

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;

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

        TermuxSessionsListViewController adapter =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", adapter);
        adapter.setEntries(publishedEntries);

        set(activity, TermuxActivity.class, "mPreferences",
            TermuxAppSharedPreferences.build(appContext, true));
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));
    }

    @Test
    public void aSessionDefinitionLoadRightAfterTheOwnerDeletedASessionLeavesItOutOfTheList()
            throws Exception {
        TermuxSession deletedByTheOwner = liveSession(DELETED_SESSION_NAME);
        shellManager.mTermuxSessions.add(deletedByTheOwner);
        TermuxSession untouched = liveSession(UNTOUCHED_SESSION_NAME);
        shellManager.mTermuxSessions.add(untouched);
        attachCurrentSession(untouched.getTerminalSession());

        activity.getTermuxTerminalSessionClient().deleteSession(deletedByTheOwner.getTerminalSession());
        assertFalse("the deletion under test must actually take the session out of the list, otherwise"
                + " the definition load afterwards would have nothing to recreate and this test would"
                + " pass without exercising anything",
            sessionNamesInTheList().contains(DELETED_SESSION_NAME));

        invokeBuildSessions(new SessionDefinitionLoadResult(publishedEntries, 1, Collections.emptyList()));

        assertFalse("the owner deletes a session to stop seeing it while the published session definition"
                + " document still lists it, because that document is only rewritten once per management"
                + " tool schedule cycle; recreating the name on the very next definition load puts the"
                + " session straight back on the owner's screen",
            sessionNamesInTheList().contains(DELETED_SESSION_NAME));
        assertTrue("a session the owner did not delete must keep its place in the list",
            sessionNamesInTheList().contains(UNTOUCHED_SESSION_NAME));
    }

    @Test
    public void aSessionDefinitionLoadAFullFifteenMinutesAfterTheDeletionPutsTheSessionBack()
            throws Exception {
        TermuxSession deletedByTheOwner = liveSession(DELETED_SESSION_NAME);
        shellManager.mTermuxSessions.add(deletedByTheOwner);
        TermuxSession untouched = liveSession(UNTOUCHED_SESSION_NAME);
        shellManager.mTermuxSessions.add(untouched);
        attachCurrentSession(untouched.getTerminalSession());

        activity.getTermuxTerminalSessionClient().deleteSession(deletedByTheOwner.getTerminalSession());
        activity.getPreferences().setUserRemovedSessionTimes(
            (System.currentTimeMillis() - UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS)
                + " " + DELETED_SESSION_NAME);

        invokeBuildSessions(new SessionDefinitionLoadResult(publishedEntries, 1, Collections.emptyList()));

        assertTrue("the deletion hides the session for fifteen minutes and no longer, so a definition"
                + " load after that window must create the name again exactly as it did before the"
                + " owner ever deleted it",
            sessionNamesInTheList().contains(DELETED_SESSION_NAME));
    }

    @Test
    public void deletingASessionRecordsWhenTheOwnerDeletedIt() throws Exception {
        TermuxSession deletedByTheOwner = liveSession(DELETED_SESSION_NAME);
        shellManager.mTermuxSessions.add(deletedByTheOwner);
        TermuxSession untouched = liveSession(UNTOUCHED_SESSION_NAME);
        shellManager.mTermuxSessions.add(untouched);
        attachCurrentSession(untouched.getTerminalSession());
        long beforeTheDeletion = System.currentTimeMillis();

        activity.getTermuxTerminalSessionClient().deleteSession(deletedByTheOwner.getTerminalSession());

        Long removedAtMillis = activity.getPreferences().getUserRemovedSessionTimes()
            .get(DELETED_SESSION_NAME);
        assertNotNull("without a recorded removal time nothing downstream can tell how long the session"
            + " has been hidden for", removedAtMillis);
        assertTrue("the recorded removal time must be the moment the owner deleted the session",
            removedAtMillis >= beforeTheDeletion && removedAtMillis <= System.currentTimeMillis());
        assertFalse("a session the owner never deleted must carry no removal time",
            activity.getPreferences().getUserRemovedSessionTimes().containsKey(UNTOUCHED_SESSION_NAME));
    }

    private List<String> sessionNamesInTheList() {
        List<String> names = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions())
            names.add(termuxSession.getTerminalSession().mSessionName);
        return names;
    }

    private void invokeBuildSessions(SessionDefinitionLoadResult result) throws Exception {
        SessionDefinitionController controller = new SessionDefinitionController(activity);
        Method buildMethod = SessionDefinitionController.class
            .getDeclaredMethod("buildSessions", SessionDefinitionLoadResult.class);
        buildMethod.setAccessible(true);
        buildMethod.invoke(controller, result);
    }

    private TermuxSession liveSession(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        TermuxSession termuxSession = constructor.newInstance(
            terminalSession, new ExecutionCommand(), null, false);
        assertNotNull(termuxSession.getTerminalSession());
        return termuxSession;
    }

    private void attachCurrentSession(TerminalSession terminalSession) throws Exception {
        com.termux.view.TerminalView terminalView =
            new com.termux.view.TerminalView(RuntimeEnvironment.getApplication(), null);
        terminalView.mTermSession = terminalSession;
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
