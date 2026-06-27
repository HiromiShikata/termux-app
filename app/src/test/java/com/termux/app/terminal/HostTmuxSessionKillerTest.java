package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HostTmuxSessionKillerTest {

    private static final class RecordingTarget implements HostTmuxSessionKiller.Target {
        final List<String> events = new ArrayList<>();
        String writtenKillCommand;

        @Override
        public void writeKillCommand(String killCommand) {
            writtenKillCommand = killCommand;
            events.add("write");
        }

        @Override
        public void finishLocalSession() {
            events.add("finish");
        }
    }

    private static final class DeferredScheduler implements HostTmuxSessionKiller.DelayScheduler {
        Runnable scheduledTask;
        long scheduledDelayMillis = -1;

        @Override
        public void scheduleAfterDelay(Runnable task, long delayMillis) {
            scheduledTask = task;
            scheduledDelayMillis = delayMillis;
        }

        void runScheduledTask() {
            if (scheduledTask != null) scheduledTask.run();
        }
    }

    @Test
    public void writesKillCommandBeforeFinishingLocalSession() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "host-session", scheduler);

        Assert.assertEquals("the kill command must be flushed to the session before any teardown is scheduled",
            List.of("write"), target.events);
        scheduler.runScheduledTask();
        Assert.assertEquals("the local session must be finished only after the kill command was written",
            List.of("write", "finish"), target.events);
    }

    @Test
    public void doesNotFinishLocalSessionUntilSchedulerFires() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "host-session", scheduler);

        Assert.assertFalse("teardown must be deferred, not run synchronously after the write",
            target.events.contains("finish"));
        Assert.assertEquals(HostTmuxSessionKiller.HOST_KILL_TRANSIT_GRACE_MILLIS, scheduler.scheduledDelayMillis);
    }

    @Test
    public void writesTmuxPrefixAndCommandPromptSequenceNotABareShellCommandLine() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "host-session", scheduler);

        Assert.assertEquals(":kill-session -t 'host-session'\n", target.writtenKillCommand);
        Assert.assertEquals("tmux must intercept the kill via its own prefix, so the first byte is the prefix key",
            0x02, target.writtenKillCommand.charAt(0));
        Assert.assertFalse("a bare shell command line would be swallowed by a foreground TUI in the pane",
            target.writtenKillCommand.startsWith("tmux "));
    }

    @Test
    public void writesProvidedPrefixKeyWhenHostCustomizedTheTmuxPrefix() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "host-session", (char) 0x01, scheduler);

        Assert.assertEquals(":kill-session -t 'host-session'\n", target.writtenKillCommand);
    }

    @Test
    public void finishesLocalSessionImmediatelyWhenSessionNameYieldsNoKillCommand() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "", scheduler);

        Assert.assertEquals(List.of("finish"), target.events);
        Assert.assertNull(target.writtenKillCommand);
        Assert.assertNull(scheduler.scheduledTask);
    }
}
