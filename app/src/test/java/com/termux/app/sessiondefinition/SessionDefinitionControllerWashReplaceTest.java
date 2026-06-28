package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
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

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionControllerWashReplaceTest {

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxSessionsListViewController adapter;

    private final List<SessionDefinitionEntry> entries = Arrays.asList(
        new SessionDefinitionEntry("projectOne", "storyA",
            Arrays.asList("https://example.test/a", "https://example.test/b")),
        new SessionDefinitionEntry("projectOne", "storyB",
            Collections.singletonList("https://example.test/c")),
        new SessionDefinitionEntry("projectTwo", "storyC",
            Collections.singletonList("https://example.test/a")));

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
        adapter.setEntries(entries);

        set(activity, TermuxActivity.class, "mPreferences",
            com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences.build(appContext, true));

        set(activity, TermuxActivity.class, "mProperties",
            com.termux.shared.termux.settings.properties.TermuxAppSharedProperties.getProperties());
    }

    @Test
    public void removeSessionsWithDisappearedDefinitionKeepsStillDefinedAndAdHocSessions() throws Exception {
        shellManager.mTermuxSessions.add(session("adhoc-local"));
        shellManager.mTermuxSessions.add(session("https://example.test/a"));
        shellManager.mTermuxSessions.add(session("https://example.test/b"));
        shellManager.mTermuxSessions.add(session("https://example.test/c"));

        invokeRemoveSessionsWithDisappearedDefinition(entries);

        List<String> remaining = remainingSessionNames();
        assertEquals(Arrays.asList("adhoc-local", "https://example.test/a",
            "https://example.test/b", "https://example.test/c"), remaining);
    }

    @Test
    public void removeSessionsWithDisappearedDefinitionRetainsSessionsNoLongerDefinedSoTheySurfaceUnderNa()
        throws Exception {
        TermuxSession adHoc = session("adhoc-local");
        TermuxSession stillDefined = session("https://example.test/a");
        TermuxSession disappearedDefinition = session("https://example.test/b");
        shellManager.mTermuxSessions.add(adHoc);
        shellManager.mTermuxSessions.add(stillDefined);
        shellManager.mTermuxSessions.add(disappearedDefinition);

        List<SessionDefinitionEntry> currentEntries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        invokeRemoveSessionsWithDisappearedDefinition(currentEntries);

        assertEquals(Arrays.asList("adhoc-local", "https://example.test/a", "https://example.test/b"),
            remainingSessionNames());
        assertTrue(service.getIndexOfSession(stillDefined.getTerminalSession()) >= 0);
        assertTrue(service.getIndexOfSession(disappearedDefinition.getTerminalSession()) >= 0);
    }

    @Test
    public void removeSessionsWithDisappearedDefinitionRetainsSessionAndKeepsActivityStore() throws Exception {
        TermuxSession stillDefined = session("https://example.test/a");
        TermuxSession disappearedDefinition = session("https://example.test/b");
        shellManager.mTermuxSessions.add(stillDefined);
        shellManager.mTermuxSessions.add(disappearedDefinition);

        com.termux.app.terminal.SessionNewActivityStore store = service.getSessionNewActivityStore();
        store.recordOutputActivity("https://example.test/a", 1_000L);
        store.recordOutputActivity("https://example.test/b", 2_000L);

        adapter.setEntries(Collections.emptyList());

        List<SessionDefinitionEntry> reloadedEntries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        invokeRemoveSessionsWithDisappearedDefinition(reloadedEntries);

        assertEquals(Arrays.asList("https://example.test/a", "https://example.test/b"), remainingSessionNames());
        assertEquals(Long.valueOf(2_000L), store.getLastOutputActivityTimeMillis("https://example.test/b"));
        assertEquals(Long.valueOf(1_000L), store.getLastOutputActivityTimeMillis("https://example.test/a"));
    }

    @Test
    public void removeSessionsWithDisappearedDefinitionRetainsAllWhenDefinitionBecomesEmpty() throws Exception {
        shellManager.mTermuxSessions.add(session("adhoc-local"));
        shellManager.mTermuxSessions.add(session("https://example.test/a"));
        shellManager.mTermuxSessions.add(session("https://example.test/c"));

        invokeRemoveSessionsWithDisappearedDefinition(Collections.emptyList());

        assertEquals(Arrays.asList("adhoc-local", "https://example.test/a", "https://example.test/c"),
            remainingSessionNames());
    }

    @Test
    public void buildSessionsKeepsFailedGroupSessionsWhenLoadIsPartialAboveSixteenSessions() throws Exception {
        int sessionCount = 22;
        List<String> allUrls = new ArrayList<>();
        TermuxSession firstSession = null;
        for (int index = 0; index < sessionCount; index++) {
            String url = "https://example.test/session/" + index;
            allUrls.add(url);
            TermuxSession liveSession = session(url);
            if (firstSession == null) {
                firstSession = liveSession;
            }
            shellManager.mTermuxSessions.add(liveSession);
        }
        attachCurrentSession(firstSession.getTerminalSession());

        int failedGroupStartIndex = 16;
        List<SessionDefinitionEntry> survivingEntries = new ArrayList<>();
        for (int index = 0; index < failedGroupStartIndex; index++) {
            survivingEntries.add(new SessionDefinitionEntry("loadedGroup", "story" + index,
                Collections.singletonList(allUrls.get(index))));
        }

        SessionDefinitionLoadResult partialResult = new SessionDefinitionLoadResult(
            survivingEntries, 2, Collections.singletonList("failedGroup"));

        invokeBuildSessions(partialResult);

        List<String> expected = new ArrayList<>(allUrls);
        expected.add("loadedGroup" + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX);
        assertEquals(expected, remainingSessionNames());
    }

    @Test
    public void buildSessionsRetainsDisappearedSessionsWhenLoadIsAuthoritative() throws Exception {
        shellManager.mTermuxSessions.add(session("adhoc-local"));
        TermuxSession stillDefined = session("https://example.test/a");
        shellManager.mTermuxSessions.add(stillDefined);
        shellManager.mTermuxSessions.add(session("https://example.test/b"));
        attachCurrentSession(stillDefined.getTerminalSession());

        List<SessionDefinitionEntry> authoritativeEntries = Collections.singletonList(
            new SessionDefinitionEntry("projectOne", "storyA",
                Collections.singletonList("https://example.test/a")));

        SessionDefinitionLoadResult authoritativeResult = new SessionDefinitionLoadResult(
            authoritativeEntries, 1, Collections.emptyList());

        invokeBuildSessions(authoritativeResult);

        assertEquals(Arrays.asList("adhoc-local", "https://example.test/a", "https://example.test/b",
            "projectOne" + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX),
            remainingSessionNames());
    }

    @Test
    public void ensureCurrentSessionValidAfterRebuildKeepsAValidCurrentSession() throws Exception {
        TermuxSession adHoc = session("adhoc-local");
        TermuxSession stillDefined = session("https://example.test/a");
        shellManager.mTermuxSessions.add(adHoc);
        shellManager.mTermuxSessions.add(stillDefined);

        attachCurrentSession(adHoc.getTerminalSession());

        invokeRemoveSessionsWithDisappearedDefinition(entries);
        activity.getTermuxTerminalSessionClient().ensureCurrentSessionValidAfterRebuild();

        assertSame(adHoc.getTerminalSession(), activity.getCurrentSession());
        assertEquals(Arrays.asList("adhoc-local", "https://example.test/a"), remainingSessionNames());
    }

    @Test
    public void rebuiltVisibleOrderPlacesAdHocBucketBeforeProjectSessions() throws Exception {
        shellManager.mTermuxSessions.add(session("adhoc-local"));
        shellManager.mTermuxSessions.add(session("https://example.test/a"));
        shellManager.mTermuxSessions.add(session("https://example.test/b"));
        adapter.notifyDataSetChanged();

        int firstVisibleSessionIndex = adapter.getFirstVisibleSessionIndexAfterRebuild();

        assertEquals(0, firstVisibleSessionIndex);
        assertEquals("adhoc-local",
            service.getTermuxSession(firstVisibleSessionIndex).getTerminalSession().mSessionName);
    }

    private void invokeRemoveSessionsWithDisappearedDefinition(List<SessionDefinitionEntry> currentEntries)
        throws Exception {
        SessionDefinitionController controller = new SessionDefinitionController(activity);
        Method removeMethod = SessionDefinitionController.class
            .getDeclaredMethod("removeSessionsWithDisappearedDefinition", List.class);
        removeMethod.setAccessible(true);
        removeMethod.invoke(controller, currentEntries);
    }

    private void invokeBuildSessions(SessionDefinitionLoadResult result) throws Exception {
        SessionDefinitionController controller = new SessionDefinitionController(activity);
        Method buildMethod = SessionDefinitionController.class
            .getDeclaredMethod("buildSessions", SessionDefinitionLoadResult.class);
        buildMethod.setAccessible(true);
        buildMethod.invoke(controller, result);
    }

    private List<String> remainingSessionNames() {
        List<String> names = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions())
            names.add(termuxSession.getTerminalSession().mSessionName);
        return names;
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
