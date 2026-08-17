package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersistedSessionSaveSchedulerTest {

    @Test
    public void serializeRunsOnBackgroundRunner() {
        List<String> events = new ArrayList<>();
        PersistedSessionSaveScheduler scheduler = new PersistedSessionSaveScheduler(
            task -> {
                events.add("background-start");
                task.run();
                events.add("background-end");
            },
            sessions -> {
                events.add("serialize");
                return "[]";
            },
            text -> events.add("write"));

        scheduler.save(Collections.emptyList());

        Assert.assertEquals(
            List.of("background-start", "serialize", "write", "background-end"), events);
    }

    @Test
    public void savePassesSnapshotToSerializer() {
        List<List<PersistedSessionRestoreData>> captured = new ArrayList<>();
        PersistedSessionSaveScheduler scheduler = new PersistedSessionSaveScheduler(
            Runnable::run,
            sessions -> {
                captured.add(sessions);
                return "[]";
            },
            text -> {
            });

        List<PersistedSessionRestoreData> snapshot = List.of(
            new PersistedSessionRestoreData("handle1", "work", "/bin/ssh", null, false, "/home"));
        scheduler.save(snapshot);

        Assert.assertEquals(1, captured.size());
        Assert.assertEquals("work", captured.get(0).get(0).getName());
        Assert.assertEquals("handle1", captured.get(0).get(0).getHandle());
    }

    @Test
    public void nullFromSerializerSkipsSink() {
        List<String> writes = new ArrayList<>();
        PersistedSessionSaveScheduler scheduler = new PersistedSessionSaveScheduler(
            Runnable::run,
            sessions -> null,
            writes::add);

        scheduler.save(Collections.emptyList());

        Assert.assertTrue(writes.isEmpty());
    }

    @Test
    public void nonNullFromSerializerCallsSink() {
        List<String> writes = new ArrayList<>();
        PersistedSessionSaveScheduler scheduler = new PersistedSessionSaveScheduler(
            Runnable::run,
            sessions -> "[{\"name\":\"work\"}]",
            writes::add);

        scheduler.save(Collections.emptyList());

        Assert.assertEquals(1, writes.size());
        Assert.assertEquals("[{\"name\":\"work\"}]", writes.get(0));
    }
}
