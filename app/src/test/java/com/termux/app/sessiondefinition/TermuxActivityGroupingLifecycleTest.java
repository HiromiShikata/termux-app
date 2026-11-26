package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityGroupingLifecycleTest {

    private static final String INDEX_URL = "https://example.test/index.json";

    @Test
    public void startRefreshesGroupingAndStopCancelsTheOneMinuteRefresh() throws Exception {
        ActivityController<TermuxActivity> controller = Robolectric.buildActivity(TermuxActivity.class);
        TermuxActivity activity = controller.get();
        Context appContext = RuntimeEnvironment.getApplication();
        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        TermuxShellManager shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        IBinder binder = service.onBind(new Intent());
        registerServiceBinder(activity, binder);

        controller.create();
        set(activity, TermuxActivity.class, "mIsInvalidState", false);
        RecordingSessionClient sessionClient = new RecordingSessionClient(activity);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", sessionClient);
        service.setTermuxTerminalSessionClient(sessionClient);
        shellManager.mTermuxSessions.add(session("https://example.test/live"));
        List<String> sessionNamesBeforeStart = sessionNames(service);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(
            appContext, true);
        preferences.setSessionDefinitionUrl(INDEX_URL);
        set(activity, TermuxActivity.class, "mPreferences", preferences);

        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.documents.put(INDEX_URL, "{\"projects\":[\"groupOne\"]}");
        fetcher.documents.put("https://example.test/groupOne.json",
            "[{\"story\":\"storyNew\",\"urls\":[\"https://example.test/not-live\"]}]");
        SessionDefinitionRepository repository = new SessionDefinitionRepository(
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser()));
        set(repository, SessionDefinitionRepository.class, "loaded", true);
        set(activity, TermuxActivity.class, "mSessionDefinitionRepository", repository);
        AtomicInteger scheduledGroupingRefreshes = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler groupingScheduler =
            new SessionDefinitionAutoReloadScheduler(() -> {
                scheduledGroupingRefreshes.incrementAndGet();
                activity.refreshSessionGroupingFromDefinition();
            });
        set(activity, TermuxActivity.class, "mSessionGroupingAutoRefresher",
            new SessionGroupingAutoRefresher(groupingScheduler,
                activity::refreshSessionGroupingFromDefinition));

        controller.start();
        awaitIndexFetchCount(fetcher, 1);
        assertEquals(1, fetcher.indexFetchCalls);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        awaitRefreshPostedWork(repository);
        assertEquals(sessionNamesBeforeStart, sessionNames(service));
        assertEquals(Collections.emptyList(), sessionClient.calls);

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MINUTES);
        awaitIndexFetchCount(fetcher, 2);
        assertEquals(2, fetcher.indexFetchCalls);
        int scheduledGroupingRefreshesBeforeStop = scheduledGroupingRefreshes.get();

        controller.stop();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MINUTES);
        assertEquals(scheduledGroupingRefreshesBeforeStop, scheduledGroupingRefreshes.get());
    }

    private static void registerServiceBinder(ContextWrapper context, IBinder binder) throws Exception {
        Method getShadowInstrumentation = Class.forName("org.robolectric.shadows.ShadowContextWrapper")
            .getDeclaredMethod("getShadowInstrumentation");
        getShadowInstrumentation.setAccessible(true);
        Object shadowInstrumentation = getShadowInstrumentation.invoke(null);
        Method setComponentNameAndServiceForBindService = shadowInstrumentation.getClass()
            .getDeclaredMethod("setComponentNameAndServiceForBindService", ComponentName.class, IBinder.class);
        setComponentNameAndServiceForBindService.setAccessible(true);
        setComponentNameAndServiceForBindService.invoke(shadowInstrumentation,
            new ComponentName(context, TermuxService.class), binder);
    }

    private static void awaitIndexFetchCount(RecordingFetcher fetcher, int expectedCount)
        throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(2000);
        while (fetcher.indexFetchCalls < expectedCount && System.nanoTime() < deadlineNanos) {
            Thread.sleep(10);
        }
    }

    private static void awaitRefreshPostedWork(SessionDefinitionRepository repository)
        throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(2000);
        while (repository.isLoading() && System.nanoTime() < deadlineNanos) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        if (repository.isLoading()) {
            throw new AssertionError("Session definition refresh callback did not complete");
        }
    }

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private volatile int indexFetchCalls;

        @Override
        public String fetch(String url) throws IOException {
            if (INDEX_URL.equals(url)) indexFetchCalls++;
            String document = documents.get(url);
            if (document == null) throw new IOException("missing document: " + url);
            return document;
        }
    }

    private static List<String> sessionNames(TermuxService service) {
        List<String> names = new ArrayList<>();
        for (TermuxSession session : service.getTermuxSessions()) {
            names.add(session.getTerminalSession().mSessionName);
        }
        return names;
    }

    private static TermuxSession session(String name) throws Exception {
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

    private static final class RecordingSessionClient extends TermuxTerminalSessionActivityClient {
        private final List<String> calls = new ArrayList<>();

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
    }
}
