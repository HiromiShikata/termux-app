package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;

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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
        RecordingPlanner planner = recordingPlanner();
        seedInitialDefinition();
        TermuxSession liveSession = session("https://example.test/live");
        shellManager.mTermuxSessions.add(liveSession);
        fetcher.documents.put(GROUP_ONE_URL, groupDocument("storyNew", "https://example.test/live"));

        invokeRefreshGroupingOnly(planner);

        assertEquals(Collections.singletonList("storyNew"), entryLabels(adapter.getEntries()));
        assertEquals(0, planner.planCalls);
        assertEquals(Collections.singletonList("https://example.test/live"), sessionNames());
    }

    @Test
    public void refreshGroupingOnlyKeepsPreviouslyDisplayedEntriesForFailedGroups()
        throws Exception {
        RecordingPlanner planner = recordingPlanner();
        seedInitialDefinitionWithTwoGroups();
        fetcher.documents.put(GROUP_ONE_URL, groupDocument("storyNew", "https://example.test/new"));
        fetcher.failedUrls.add(GROUP_TWO_URL);

        invokeRefreshGroupingOnly(planner);

        assertEquals(Arrays.asList("storyNew", "storyOld"), entryLabels(adapter.getEntries()));
        assertEquals(0, planner.planCalls);
    }

    @Test
    public void refreshGroupingOnlyDoesNotRemoveASessionWhoseDefinitionDisappeared() throws Exception {
        RecordingPlanner planner = recordingPlanner();
        seedInitialDefinition();
        TermuxSession disappearedSession = session("https://example.test/disappeared");
        shellManager.mTermuxSessions.add(disappearedSession);
        fetcher.documents.put(INDEX_URL, "{\"projects\":[]}");

        invokeRefreshGroupingOnly(planner);

        assertEquals(Collections.singletonList("https://example.test/disappeared"), sessionNames());
        assertEquals(0, planner.planCalls);
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
            new com.termux.app.terminal.TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());
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
        repository.loadForRebuild(INDEX_URL, (result) -> adapter.setEntries(result.getEntries()),
            (exception) -> { throw new AssertionError(exception); });
        flushMainLooper();
    }

    private void invokeRefreshGroupingOnly(RecordingPlanner planner) throws Exception {
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : SessionDefinitionController.class.getDeclaredConstructors()) {
            if (candidate.getParameterTypes().length == 2) {
                constructor = candidate;
                break;
            }
        }
        if (constructor == null) throw new AssertionError("the controller must accept a planner fake");
        constructor.setAccessible(true);
        Object controller = constructor.newInstance(activity, planner.value);
        Method refresh = SessionDefinitionController.class.getDeclaredMethod("refreshGroupingOnly");
        refresh.setAccessible(true);
        refresh.invoke(controller);
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

    private void flushMainLooper() {
        for (int attempt = 0; attempt < 200; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
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

        @Override
        public String fetch(String url) throws IOException {
            if (failedUrls.contains(url)) throw new IOException("simulated group failure");
            String document = documents.get(url);
            if (document == null) throw new IOException("missing document: " + url);
            return document;
        }
    }

    private RecordingPlanner recordingPlanner() throws Exception {
        Constructor<?>[] constructors = SessionDefinitionController.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[1].isInterface()) {
                RecordingPlanner recordingPlanner = new RecordingPlanner();
                recordingPlanner.value = Proxy.newProxyInstance(parameterTypes[1].getClassLoader(),
                    new Class<?>[]{parameterTypes[1]}, recordingPlanner);
                return recordingPlanner;
            }
        }
        throw new AssertionError("the controller must accept a planner fake");
    }

    private static final class RecordingPlanner implements InvocationHandler {
        private Object value;
        private int planCalls;

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if ("plan".equals(method.getName())) planCalls++;
            return null;
        }
    }
}
