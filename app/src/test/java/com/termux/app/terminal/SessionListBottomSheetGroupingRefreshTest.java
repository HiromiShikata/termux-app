package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.sessiondefinition.SessionDefinitionDocumentFetcher;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.app.sessiondefinition.SessionDefinitionParser;
import com.termux.app.sessiondefinition.SessionDefinitionRepository;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SessionListBottomSheetGroupingRefreshTest {

    private static final String INDEX_URL = "https://example.test/index.json";

    private TermuxActivity activity;
    private MutableFetcher fetcher;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(TermuxActivity.class).get();
        activity.setContentView(LayoutInflater.from(activity).inflate(
            R.layout.activity_termux, new FrameLayout(activity), false));
        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        Context appContext = RuntimeEnvironment.getApplication();
        TermuxShellManager shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mPreferences", TermuxAppSharedPreferences.build(appContext, true));
        set(activity, TermuxActivity.class, "mTermuxSessionListViewController",
            new TermuxSessionsListViewController(activity, service.getTermuxSessions()));
        fetcher = new MutableFetcher();
        fetcher.documents.put(INDEX_URL, "{\"projects\":[]}");
        set(activity, TermuxActivity.class, "mSessionDefinitionRepository",
            new SessionDefinitionRepository(new SessionDefinitionLoader(fetcher, new SessionDefinitionParser())));
        loadDefinition();
    }

    @Test
    public void eachShowFetchesTheDefinitionIndexOnceForEachNonBlankUrl() throws Exception {
        String[] definitionUrls = {INDEX_URL, INDEX_URL, "", INDEX_URL, null, "   ", INDEX_URL};
        int expectedIndexFetches = 0;

        for (String definitionUrl : definitionUrls) {
            TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(
                RuntimeEnvironment.getApplication(), true);
            if (definitionUrl == null) {
                preferences.getSharedPreferences().edit().remove("session_definition_url").apply();
            } else {
                preferences.setSessionDefinitionUrl(definitionUrl);
            }
            set(activity, TermuxActivity.class, "mPreferences", preferences);

            new SessionListBottomSheetController(activity).show();
            drainMainLooper();

            if (definitionUrl != null && !definitionUrl.trim().isEmpty()) expectedIndexFetches++;
            assertEquals(expectedIndexFetches, fetcher.indexFetchCalls);
        }
    }

    private void loadDefinition() throws Exception {
        SessionDefinitionRepository repository = (SessionDefinitionRepository) get(
            activity, TermuxActivity.class, "mSessionDefinitionRepository");
        repository.loadForRebuild(INDEX_URL, false, result -> { }, exception -> {
            throw new AssertionError(exception);
        });
        drainMainLooper();
        fetcher.fetchCalls = 0;
        fetcher.indexFetchCalls = 0;
    }

    private static void drainMainLooper() throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
    }

    private static Object get(Object target, Class<?> declaringClass, String fieldName) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class MutableFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private int fetchCalls;
        private int indexFetchCalls;

        @Override
        public String fetch(String url) throws IOException {
            fetchCalls++;
            if (INDEX_URL.equals(url)) indexFetchCalls++;
            String document = documents.get(url);
            if (document == null) throw new IOException("missing document: " + url);
            return document;
        }
    }
}
