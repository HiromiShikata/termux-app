package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

@RunWith(RobolectricTestRunner.class)
public class FileSessionNewActivityPersistenceTest {

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
    public void loadOfMissingCacheFileReturnsCleanState() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        List<SessionNewActivityState> states = persistence.load();

        Assert.assertTrue(states.isEmpty());
    }

    @Test
    public void loadOfEmptyCacheFileReturnsCleanState() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Files.write(cacheFile.toPath(), "".getBytes(StandardCharsets.UTF_8));
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        List<SessionNewActivityState> states = persistence.load();

        Assert.assertTrue(states.isEmpty());
    }

    @Test
    public void saveThenLoadRoundTripsState() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());

        persistence.save(Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, "needs approval", 3_000L, 4_000L)));

        List<SessionNewActivityState> reloaded = persistence.load();
        Assert.assertEquals(1, reloaded.size());
        Assert.assertEquals("session-one", reloaded.get(0).getSessionName());
        Assert.assertEquals("needs approval", reloaded.get(0).getLastExplicitCallReason());
    }

    @Test
    public void saveCapsOversizedReasonInCacheFile() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(cacheFile, new DirectExecutor());
        StringBuilder oversizedReason = new StringBuilder();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASON_LENGTH + 2_000; index++) {
            oversizedReason.append('x');
        }

        persistence.save(Arrays.asList(
            new SessionNewActivityState("session-one", 1_000L, 2_000L, oversizedReason.toString(),
                3_000L, 4_000L)));

        List<SessionNewActivityState> reloaded = persistence.load();
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH,
            reloaded.get(0).getLastExplicitCallReason().length());
    }
}
