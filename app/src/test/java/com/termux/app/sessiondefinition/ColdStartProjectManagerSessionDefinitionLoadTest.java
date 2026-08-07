package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Looper;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
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
import org.robolectric.Shadows;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class ColdStartProjectManagerSessionDefinitionLoadTest {

    private static final String INDEX_URL = "https://example.test/base/index.json";
    private static final String FIRST_PROJECT_MANAGER_SESSION_NAME = "groupOnepm";
    private static final String SECOND_PROJECT_MANAGER_SESSION_NAME = "groupTwopm";
    private static final int SESSION_CAP = 16;

    private TermuxActivity activity;
    private TermuxService service;
    private TermuxAppSharedPreferences preferences;
    private CountingFetcher fetcher;
    private SessionDefinitionRepository repository;

    private static final class CountingFetcher implements SessionDefinitionDocumentFetcher {

        private final Map<String, String> documentsByUrl = new HashMap<>();
        private final AtomicInteger indexFetchCount = new AtomicInteger();

        private void register(String url, String document) {
            documentsByUrl.put(url, document);
        }

        @Override
        public String fetch(String url) throws IOException {
            if (url.equals(INDEX_URL)) {
                indexFetchCount.incrementAndGet();
            }
            String document = documentsByUrl.get(url);
            if (document == null) {
                throw new IOException("No document registered for " + url);
            }
            return document;
        }
    }

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        service = Robolectric.buildService(TermuxService.class).get();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(appContext));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        TermuxTerminalSessionActivityClient sessionClient = new TermuxTerminalSessionActivityClient(activity);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient", sessionClient);
        service.setTermuxTerminalSessionClient(sessionClient);
        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setAutosshCommand("ssh {name}");
        preferences.setSessionDefinitionMaxSessions(SESSION_CAP);
        preferences.setSessionDefinitionUrl(INDEX_URL);

        TermuxSessionsListViewController listViewController =
            new TermuxSessionsListViewController(activity, service.getTermuxSessions());
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController", listViewController);
        listViewController.setEntries(Collections.emptyList());

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        set(activity, TermuxActivity.class, "mTerminalView", new TerminalView(appContext, null));

        fetcher = new CountingFetcher();
        fetcher.register(INDEX_URL, "{\"projects\":[\"groupOne\",\"groupTwo\"]}");
        fetcher.register("https://example.test/base/groupOne.json",
            "[{\"story\":\"storyA\",\"urls\":[\"https://example.test/a\"]}]");
        fetcher.register("https://example.test/base/groupTwo.json",
            "[{\"story\":\"storyB\",\"urls\":[\"https://example.test/b\"]}]");
        repository = new SessionDefinitionRepository(
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser()));
    }

    @Test
    public void anEmptyEntryCacheTriggersOneDefinitionLoadThatCreatesEveryProjectManagerSession()
            throws Exception {
        assertTrue("the cold-start arrangement must start with no cached entries, because the entry cache "
            + "does not survive a process restart", repository.getCachedEntries().isEmpty());

        controller().restoreProjectManagerSessionsOnColdStart();
        flushMainLooper();

        assertEquals("exactly one session definition load must be issued on a cold start", 1,
            fetcher.indexFetchCount.get());
        assertNotNull("every project's project-manager session must be created from the cold-start "
                + "definition load without the owner pressing Load Sessions; the live session names were "
                + liveSessionNames(), service.getTermuxSessionForSessionName(FIRST_PROJECT_MANAGER_SESSION_NAME));
        assertNotNull("every project's project-manager session must be created from the cold-start "
                + "definition load without the owner pressing Load Sessions; the live session names were "
                + liveSessionNames(), service.getTermuxSessionForSessionName(SECOND_PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aDefinitionLoadAlreadyInFlightForTheSessionListStillCreatesEveryProjectManagerSession()
            throws Exception {
        AtomicInteger sessionListGroupingNotificationCount = new AtomicInteger();
        repository.load(INDEX_URL, sessionListGroupingNotificationCount::incrementAndGet);

        controller().restoreProjectManagerSessionsOnColdStart();
        flushMainLooper();

        assertEquals("the definition document must be fetched once even though the session list grouping and "
            + "the cold-start session creation both wait for it", 1, fetcher.indexFetchCount.get());
        assertEquals("the session list grouping listener must still be notified", 1,
            sessionListGroupingNotificationCount.get());
        assertNotNull("a definition load already in flight for the session list must not swallow the "
                + "cold-start project-manager session creation; the live session names were "
                + liveSessionNames(), service.getTermuxSessionForSessionName(FIRST_PROJECT_MANAGER_SESSION_NAME));
        assertNotNull("a definition load already in flight for the session list must not swallow the "
                + "cold-start project-manager session creation; the live session names were "
                + liveSessionNames(), service.getTermuxSessionForSessionName(SECOND_PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aHiddenProjectManagerSessionNameStaysUncreatedOnAColdStart() throws Exception {
        preferences.setDisabledSessionNames(TermuxAppSharedPreferences.serializeDisabledSessionNames(
            new LinkedHashSet<>(Collections.singletonList(FIRST_PROJECT_MANAGER_SESSION_NAME))));

        controller().restoreProjectManagerSessionsOnColdStart();
        flushMainLooper();

        assertTrue("the arrangement must record the name as hidden before the assertion below means "
                + "anything; the stored hidden set was " + preferences.getDisabledSessionNames(),
            preferences.getDisabledSessionNames().contains(FIRST_PROJECT_MANAGER_SESSION_NAME));
        assertEquals("a project-manager name the owner explicitly hid must stay uncreated on a cold start; "
                + "the live session names were " + liveSessionNames(),
            Collections.singletonList(SECOND_PROJECT_MANAGER_SESSION_NAME), liveSessionNames());
    }

    private SessionDefinitionController controller() {
        return new SessionDefinitionController(activity, repository, new SessionDefinitionPlanner());
    }

    private List<String> liveSessionNames() {
        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }
        return liveSessionNames;
    }

    private void flushMainLooper() throws InterruptedException {
        for (int attempt = 0; attempt < 200; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
