package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.os.Looper;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxSessionsListViewController;
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
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowToast;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SessionGroupingRefreshTest {

    private static final String INDEX_URL = "https://example.test/base/index.json";
    private static final String GROUP_ONE_URL = "https://example.test/base/groupOne.json";
    private static final String GROUP_TWO_URL = "https://example.test/base/groupTwo.json";

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxShellManager shellManager;
    private TermuxSessionsListViewController adapter;
    private MutableFetcher fetcher;
    private RecordingSessionClient sessionClient;

    @Test
    public void refreshIfDefinedRunsExactlyOnceOnlyForDefinedUrls() {
        String[] definitionUrls = {null, "", "   ", INDEX_URL};
        boolean[] expectedRefresh = {false, false, false, true};

        for (int index = 0; index < definitionUrls.length; index++) {
            final int[] refreshCount = {0};
            SessionGroupingRefreshGate.refreshIfDefined(definitionUrls[index],
                () -> refreshCount[0]++);
            assertEquals(expectedRefresh[index] ? 1 : 0, refreshCount[0]);
        }
    }

    @Test
    public void shouldRefreshGroupingUsesOnlyNonBlankDefinitionUrls() {
        String[] definitionUrls = {null, "", "   ", INDEX_URL};
        boolean[] expected = {false, false, false, true};

        for (int index = 0; index < definitionUrls.length; index++) {
            assertEquals(expected[index],
                SessionGroupingRefreshGate.shouldRefreshGrouping(definitionUrls[index]));
        }
    }

    @Test
    public void groupingRefreshIntervalIsOneMinute() {
        assertEquals(1, SessionGroupingRefreshGate.GROUPING_REFRESH_INTERVAL_MINUTES);
    }

    @Test
    public void refreshGroupingOnlyUpdatesDisplayedGroupingWithoutPlanningOrChangingSessions()
        throws Exception {
        seedInitialDefinition();
        TermuxSession liveSession = session("https://example.test/live");
        shellManager.mTermuxSessions.add(liveSession);
        fetcher.documents.put(GROUP_ONE_URL, groupDocument("storyNew", "https://example.test/live"));

        invokeRefreshGroupingOnly();

        assertEquals(Collections.singletonList("storyNew"), entryLabels(adapter.getEntries()));
        assertEquals(Collections.singletonList("https://example.test/live"), sessionNames());
        assertEquals(Collections.emptyList(), sessionClient.calls);
    }

    @Test
    public void refreshGroupingOnlyBeforeInitialLoadDelegatesToFirstLoadWithoutChangingSessions()
        throws Exception {
        adapter.setEntries(Collections.singletonList(
            new SessionDefinitionEntry("groupOne", "storyOld",
                Collections.singletonList("https://example.test/live"))));
        TermuxSession liveSession = session("https://example.test/live");
        shellManager.mTermuxSessions.add(liveSession);
        fetcher.documents.put(INDEX_URL, "{\"projects\":[\"groupOne\"]}");
        fetcher.documents.put(GROUP_ONE_URL,
            groupDocument("storyNew", "https://example.test/live"));

        invokeRefreshGroupingOnly();

        assertEquals(1, Collections.frequency(fetcher.fetchedUrls, INDEX_URL));
        assertEquals(Collections.singletonList("storyNew"), entryLabels(adapter.getEntries()));
        assertEquals(Collections.singletonList("https://example.test/live"), sessionNames());
        assertEquals(Collections.emptyList(), sessionClient.calls);
    }

    @Test
    public void refreshGroupingOnlyKeepsPreviouslyDisplayedEntriesForFailedGroups()
        throws Exception {
        seedInitialDefinitionWithTwoGroups();
        fetcher.documents.put(GROUP_ONE_URL, groupDocument("storyNew", "https://example.test/new"));
        fetcher.failedUrls.add(GROUP_TWO_URL);

        invokeRefreshGroupingOnly();

        assertEquals(Arrays.asList("storyNew", "storyOld"), entryLabels(adapter.getEntries()));
        assertEquals(Collections.emptyList(), sessionClient.calls);
        assertEquals(1, ShadowToast.shownToastCount());

        invokeRefreshGroupingOnly();

        assertEquals(1, ShadowToast.shownToastCount());
    }

    @Test
    public void refreshGroupingOnlyShowsFailureToastOnEachFailureTransition() throws Exception {
        seedInitialDefinition();
        activity.setGroupingRefreshFailing(false);
        fetcher.failedUrls.add(GROUP_ONE_URL);

        invokeRefreshGroupingOnly();
        invokeRefreshGroupingOnly();

        assertEquals(1, ShadowToast.shownToastCount());

        fetcher.failedUrls.clear();
        invokeRefreshGroupingOnly();
        assertFalse(activity.isGroupingRefreshFailing());

        fetcher.failedUrls.add(GROUP_ONE_URL);
        invokeRefreshGroupingOnly();

        assertEquals(2, ShadowToast.shownToastCount());
    }

    @Test
    public void refreshGroupingOnlyDoesNotRemoveASessionWhoseDefinitionDisappeared() throws Exception {
        seedInitialDefinition();
        TermuxSession disappearedSession = session("https://example.test/disappeared");
        shellManager.mTermuxSessions.add(disappearedSession);
        fetcher.documents.put(INDEX_URL, "{\"projects\":[]}");

        invokeRefreshGroupingOnly();

        assertEquals(Collections.singletonList("https://example.test/disappeared"), sessionNames());
        assertEquals(Collections.emptyList(), sessionClient.calls);
    }

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
            sessionClient = new RecordingSessionClient(activity));
        service.setTermuxTerminalSessionClient(sessionClient);
        adapter = new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", adapter);
        set(activity, TermuxActivity.class, "mPreferences",
            TermuxAppSharedPreferences.build(appContext, true));
        fetcher = new MutableFetcher();
        set(activity, TermuxActivity.class, "mSessionDefinitionRepository",
            new SessionDefinitionRepository(new SessionDefinitionLoader(fetcher,
                new SessionDefinitionParser())));
        activity.getPreferences().setSessionDefinitionUrl(INDEX_URL);
    }

    private void seedInitialDefinition() throws Exception {
        fetcher.documents.put(INDEX_URL, "{\"projects\":[\"groupOne\"]}");
        fetcher.documents.put(GROUP_ONE_URL,
            groupDocument("storyOld", "https://example.test/live"));
        loadInitialDefinition();
    }

    private void seedInitialDefinitionWithTwoGroups() throws Exception {
        fetcher.documents.put(INDEX_URL, "{\"projects\":[\"groupOne\",\"groupTwo\"]}");
        fetcher.documents.put(GROUP_ONE_URL, groupDocument("storyOld", "https://example.test/live"));
        fetcher.documents.put(GROUP_TWO_URL, groupDocument("storyOld", "https://example.test/old"));
        loadInitialDefinition();
    }

    private void loadInitialDefinition() throws Exception {
        SessionDefinitionRepository repository = (SessionDefinitionRepository) get(
            activity, TermuxActivity.class, "mSessionDefinitionRepository");
        repository.loadForRebuild(INDEX_URL, false,
            (result) -> adapter.setEntries(result.getEntries()),
            (exception) -> { throw new AssertionError(exception); });
        flushMainLooper();
    }

    private void invokeRefreshGroupingOnly() throws Exception {
        SessionDefinitionRepository repository = (SessionDefinitionRepository) get(
            activity, TermuxActivity.class, "mSessionDefinitionRepository");
        new SessionDefinitionController(activity, repository, new SessionDefinitionPlanner())
            .refreshGroupingOnly();
        flushMainLooper();
    }

    private static String groupDocument(String story, String sessionName) {
        return "[{\"story\":\"" + story + "\",\"urls\":[\"" + sessionName + "\"]}]";
    }

    private List<String> entryLabels(List<SessionDefinitionEntry> entries) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (SessionDefinitionEntry entry : entries) labels.add(entry.getEntryLabel());
        return labels;
    }

    private List<String> sessionNames() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        for (TermuxSession session : service.getTermuxSessions()) {
            names.add(session.getTerminalSession().mSessionName);
        }
        return names;
    }

    private TermuxSession session(String name) throws Exception {
        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null, null);
        terminalSession.mSessionName = name;
        Field shellPid = TerminalSession.class.getDeclaredField("mShellPid");
        shellPid.setAccessible(true);
        shellPid.setInt(terminalSession, -1);
        Constructor<TermuxSession> constructor = TermuxSession.class.getDeclaredConstructor(
            TerminalSession.class, ExecutionCommand.class, TermuxSession.TermuxSessionClient.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(terminalSession, new ExecutionCommand(), null, false);
    }

    private void flushMainLooper() throws InterruptedException {
        for (int attempt = 0; attempt < 200; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
    }

    private static Object get(Object target, Class<?> declaringClass, String fieldName)
        throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value)
        throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class MutableFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private final java.util.Set<String> failedUrls = new java.util.HashSet<>();
        private final java.util.ArrayList<String> fetchedUrls = new java.util.ArrayList<>();
        private int fetchCalls;

        @Override
        public String fetch(String url) throws IOException {
            fetchCalls++;
            fetchedUrls.add(url);
            if (failedUrls.contains(url)) throw new IOException("simulated group failure");
            String document = documents.get(url);
            if (document == null) throw new IOException("missing document: " + url);
            return document;
        }
    }

    private static final class RecordingSessionClient
        extends com.termux.app.terminal.TermuxTerminalSessionActivityClient {
        private final java.util.ArrayList<String> calls = new java.util.ArrayList<>();

        private RecordingSessionClient(TermuxActivity activity) {
            super(activity);
        }

        @Override
        public void addNewSession(boolean isFailSafe, String sessionName, boolean closeDrawerAfter) {
            calls.add("addNewSession");
            super.addNewSession(isFailSafe, sessionName, closeDrawerAfter);
        }

        @Override
        public void addNewAutosshSession(String sessionName, String command, boolean closeDrawerAfter) {
            calls.add("addNewAutosshSession");
            super.addNewAutosshSession(sessionName, command, closeDrawerAfter);
        }

        @Override
        public void removeSessionForRebuild(TerminalSession sessionToRemove) {
            calls.add("removeSession");
            super.removeSessionForRebuild(sessionToRemove);
        }

        @Override
        public void restoreDisplayedSessionAfterReloadIfStillLive(TerminalSession sessionBeforeReload) {
            calls.add("restoreDisplayedSessionAfterReloadIfStillLive");
            super.restoreDisplayedSessionAfterReloadIfStillLive(sessionBeforeReload);
        }
    }
}
