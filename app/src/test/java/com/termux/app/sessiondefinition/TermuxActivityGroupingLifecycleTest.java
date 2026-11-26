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
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.shell.TermuxShellManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class TermuxActivityGroupingLifecycleTest {

    private static final String INDEX_URL = "https://example.test/index.json";

    @Test
    public void startRefreshesGroupingAndStopCancelsTheOneMinuteRefresh() throws Exception {
        ActivityController<TermuxActivity> controller = Robolectric.buildActivity(TermuxActivity.class);
        TermuxActivity activity = controller.get();
        Context appContext = RuntimeEnvironment.getApplication();
        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(appContext));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
        IBinder binder = service.onBind(new Intent());
        registerServiceBinder(activity, binder);

        controller.create();
        set(activity, TermuxActivity.class, "mIsInvalidState", false);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(
            appContext, true);
        preferences.setSessionDefinitionUrl(INDEX_URL);
        set(activity, TermuxActivity.class, "mPreferences", preferences);

        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.documents.put(INDEX_URL, "{\"projects\":[]}");
        SessionDefinitionRepository repository = new SessionDefinitionRepository(
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser()));
        set(repository, SessionDefinitionRepository.class, "loaded", true);
        set(activity, TermuxActivity.class, "mSessionDefinitionRepository", repository);

        controller.start();
        awaitFetch(fetcher);
        assertEquals(1, fetcher.indexFetchCalls);

        controller.stop();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MINUTES);
        assertEquals(1, fetcher.indexFetchCalls);
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
