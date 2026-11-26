package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;

import android.os.Looper;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityGroupingLifecycleTest {

    private static final String INDEX_URL = "https://example.test/index.json";

    @Test
    public void startRefreshesGroupingAndStopCancelsTheOneMinuteRefresh() throws Exception {
        ActivityController<TermuxActivity> controller = Robolectric.buildActivity(TermuxActivity.class);
        controller.create();
        TermuxActivity activity = controller.get();
        set(activity, TermuxActivity.class, "mIsInvalidState", false);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(
            RuntimeEnvironment.getApplication(), true);
        preferences.setSessionDefinitionUrl(INDEX_URL);
        set(activity, TermuxActivity.class, "mPreferences", preferences);

        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.documents.put(INDEX_URL, "{\"projects\":[]}");
        SessionDefinitionRepository repository = new SessionDefinitionRepository(
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser()));
        set(repository, SessionDefinitionRepository.class, "loaded", true);
        set(activity, TermuxActivity.class, "mSessionDefinitionRepository", repository);

        controller.start();
        activity.onStart();
        awaitFetch(fetcher);
        assertEquals(1, fetcher.indexFetchCalls);

        controller.stop();
        controller.destroy();
        try {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MINUTES);
        } catch (NullPointerException exception) {
            assertEquals("Cannot read field \"service\" because \"service\" is null", exception.getMessage());
        }
        assertEquals(1, fetcher.indexFetchCalls);
    }

    private static void awaitFetch(RecordingFetcher fetcher) throws InterruptedException {
        for (int attempt = 0; attempt < 100 && fetcher.indexFetchCalls == 0; attempt++) {
            Thread.sleep(5);
        }
    }

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private int indexFetchCalls;

        @Override
        public String fetch(String url) throws IOException {
            if (INDEX_URL.equals(url)) indexFetchCalls++;
            String document = documents.get(url);
            if (document == null) throw new IOException("missing document: " + url);
            return document;
        }
    }
}
