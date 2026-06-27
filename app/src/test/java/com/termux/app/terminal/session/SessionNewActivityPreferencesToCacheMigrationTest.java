package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;

@RunWith(RobolectricTestRunner.class)
public class SessionNewActivityPreferencesToCacheMigrationTest {

    private static final class RecordingPreferencesStore implements SessionNewActivityStateStore {

        private String mValue;
        private boolean mSetCalled;

        RecordingPreferencesStore(String initialValue) {
            mValue = initialValue;
        }

        @Override
        public String getPersistedSessionNewActivityStates() {
            return mValue;
        }

        @Override
        public void setPersistedSessionNewActivityStates(String value) {
            mSetCalled = true;
            mValue = value;
        }
    }

    private static final class DirectExecutor implements ExecutorService {
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() {}
        @Override public java.util.List<Runnable> shutdownNow() { return java.util.Collections.emptyList(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return true; }
        @Override public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) { throw new UnsupportedOperationException(); }
        @Override public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) { throw new UnsupportedOperationException(); }
        @Override public java.util.concurrent.Future<?> submit(Runnable task) { throw new UnsupportedOperationException(); }
        @Override public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) { throw new UnsupportedOperationException(); }
    }

    @Test
    public void migrationWritesCacheFileAndRemovesLegacyPreferencesKey() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        String legacyValue = "[{\"sessionName\":\"session-one\",\"lastExplicitCallTimeMillis\":2000,"
            + "\"lastExplicitCallReason\":\"needs approval\"}]";
        RecordingPreferencesStore preferencesStore = new RecordingPreferencesStore(legacyValue);
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        new SessionNewActivityPreferencesToCacheMigration(preferencesStore, persistence).migrate();

        Assert.assertTrue(cacheFile.exists());
        String cacheContent = new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8);
        Assert.assertTrue(cacheContent.contains("session-one"));
        Assert.assertNull(preferencesStore.getPersistedSessionNewActivityStates());

        List<SessionNewActivityState> reloaded = persistence.load();
        Assert.assertEquals(1, reloaded.size());
        Assert.assertEquals("needs approval", reloaded.get(0).getLastExplicitCallReason());
    }

    @Test
    public void migrationShrinksOversizedLegacyValueIntoCache() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        StringBuilder oversizedReason = new StringBuilder();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASON_LENGTH + 10_000; index++) {
            oversizedReason.append('x');
        }
        String legacyValue = "[{\"sessionName\":\"session-one\",\"lastExplicitCallReason\":\""
            + oversizedReason + "\"}]";
        RecordingPreferencesStore preferencesStore = new RecordingPreferencesStore(legacyValue);
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        new SessionNewActivityPreferencesToCacheMigration(preferencesStore, persistence).migrate();

        List<SessionNewActivityState> reloaded = persistence.load();
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH,
            reloaded.get(0).getLastExplicitCallReason().length());
        Assert.assertNull(preferencesStore.getPersistedSessionNewActivityStates());
    }

    @Test
    public void migratedStateIsVisibleToAStoreLoadedFromTheSameCacheFile() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        String legacyValue = "[{\"sessionName\":\"session-one\",\"lastExplicitCallTimeMillis\":2000,"
            + "\"lastExplicitCallReason\":\"needs approval\","
            + "\"unacknowledgedCallReasons\":[\"needs approval\"]}]";
        RecordingPreferencesStore preferencesStore = new RecordingPreferencesStore(legacyValue);
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        new SessionNewActivityPreferencesToCacheMigration(preferencesStore, persistence).migrate();
        com.termux.app.terminal.SessionNewActivityStore store =
            new com.termux.app.terminal.SessionNewActivityStore(persistence);

        Assert.assertEquals("needs approval", store.getLastExplicitCallReason("session-one"));
        Assert.assertEquals(1, store.getUnacknowledgedCallReasons("session-one").size());
    }

    @Test
    public void migrationDoesNothingWhenLegacyValueEmpty() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        RecordingPreferencesStore preferencesStore = new RecordingPreferencesStore("");
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        new SessionNewActivityPreferencesToCacheMigration(preferencesStore, persistence).migrate();

        Assert.assertFalse(cacheFile.exists());
        Assert.assertEquals("", preferencesStore.getPersistedSessionNewActivityStates());
    }
}
