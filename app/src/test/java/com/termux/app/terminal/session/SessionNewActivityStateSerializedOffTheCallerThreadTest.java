package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class SessionNewActivityStateSerializedOffTheCallerThreadTest {

    private static final String WRITE_THREAD_NAME = "session-activity-state-write";

    private static final class QueuedExecutor implements ExecutorService {
        private final List<Runnable> mQueued = new ArrayList<>();

        int queuedCount() {
            return mQueued.size();
        }

        void runQueued() {
            List<Runnable> queued = new ArrayList<>(mQueued);
            mQueued.clear();
            for (Runnable runnable : queued) {
                runnable.run();
            }
        }

        @Override public void execute(Runnable command) { mQueued.add(command); }
        @Override public void shutdown() {}
        @Override public List<Runnable> shutdownNow() { return Collections.emptyList(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
        @Override public <T> Future<T> submit(Callable<T> task) { throw new UnsupportedOperationException(); }
        @Override public <T> Future<T> submit(Runnable task, T result) { throw new UnsupportedOperationException(); }
        @Override public Future<?> submit(Runnable task) { throw new UnsupportedOperationException(); }
        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
    }

    private static final class RecordingSerialization implements SessionNewActivityStateSerialization {

        private final SessionNewActivityStateSerializer mSerializer = new SessionNewActivityStateSerializer();
        private final List<String> mSerializedSessionNames = new ArrayList<>();
        private final List<String> mThreadNames = new ArrayList<>();

        @Override
        public String serialize(List<SessionNewActivityState> states) throws JSONException {
            StringBuilder sessionNames = new StringBuilder();
            for (SessionNewActivityState state : states) {
                if (sessionNames.length() > 0) {
                    sessionNames.append(',');
                }
                sessionNames.append(state.getSessionName());
            }
            synchronized (this) {
                mSerializedSessionNames.add(sessionNames.toString());
                mThreadNames.add(Thread.currentThread().getName());
            }
            return mSerializer.serialize(states);
        }

        @Override
        public List<SessionNewActivityState> deserialize(String serialized) throws JSONException {
            return mSerializer.deserialize(serialized);
        }

        synchronized int serializeCount() {
            return mSerializedSessionNames.size();
        }

        synchronized String lastSerializedSessionNames() {
            return mSerializedSessionNames.get(mSerializedSessionNames.size() - 1);
        }

        synchronized String lastThreadName() {
            return mThreadNames.get(mThreadNames.size() - 1);
        }
    }

    private static List<SessionNewActivityState> statesNamed(String sessionName) {
        return Arrays.asList(new SessionNewActivityState(sessionName, 1_000L, 2_000L, "", 3_000L, 4_000L));
    }

    private static File emptyCacheFile() throws Exception {
        File cacheFile = File.createTempFile("session_new_activity_states", ".json");
        Assert.assertTrue(cacheFile.delete());
        return cacheFile;
    }

    @Test
    public void recordingActivityDoesNotSerializeBeforeItReturnsToTheCaller() throws Exception {
        QueuedExecutor writeExecutor = new QueuedExecutor();
        RecordingSerialization serialization = new RecordingSerialization();
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(emptyCacheFile(), writeExecutor, serialization);

        persistence.save(statesNamed("session-one"));

        Assert.assertEquals("this call runs on the main thread on every genuine shell output, so serializing"
                + " the state of every session inside it stops the terminal from drawing",
            0, serialization.serializeCount());
        Assert.assertEquals(1, writeExecutor.queuedCount());

        writeExecutor.runQueued();

        Assert.assertEquals(1, serialization.serializeCount());
        Assert.assertEquals("session-one", serialization.lastSerializedSessionNames());
    }

    @Test
    public void furtherActivityArrivingBeforeTheWriteRunsIsSerializedOnceWithTheLatestState()
            throws Exception {
        QueuedExecutor writeExecutor = new QueuedExecutor();
        RecordingSerialization serialization = new RecordingSerialization();
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(emptyCacheFile(), writeExecutor, serialization);

        persistence.save(statesNamed("session-one"));
        persistence.save(statesNamed("session-two"));
        persistence.save(statesNamed("session-three"));

        Assert.assertEquals("output arrives in bursts, so the state is superseded far more often than it"
                + " reaches the file, and serializing every superseded state is work nobody reads",
            0, serialization.serializeCount());

        writeExecutor.runQueued();

        Assert.assertEquals(1, serialization.serializeCount());
        Assert.assertEquals("session-three", serialization.lastSerializedSessionNames());
    }

    @Test
    public void theStateIsSerializedOnTheWriteThreadRatherThanTheThreadThatRecordedTheActivity()
            throws Exception {
        ThreadFactory writeThreadFactory = runnable -> new Thread(runnable, WRITE_THREAD_NAME);
        ExecutorService writeExecutor = Executors.newSingleThreadExecutor(writeThreadFactory);
        RecordingSerialization serialization = new RecordingSerialization();
        FileSessionNewActivityPersistence persistence =
            new FileSessionNewActivityPersistence(emptyCacheFile(), writeExecutor, serialization);
        String recordingThreadName = Thread.currentThread().getName();

        persistence.save(statesNamed("session-one"));
        writeExecutor.shutdown();
        Assert.assertTrue(writeExecutor.awaitTermination(5, TimeUnit.SECONDS));

        Assert.assertEquals("the caller is the main thread in the running app, and the longest single stall"
                + " measured on the device was captured inside this serialization",
            WRITE_THREAD_NAME, serialization.lastThreadName());
        Assert.assertNotEquals(recordingThreadName, serialization.lastThreadName());
    }
}
