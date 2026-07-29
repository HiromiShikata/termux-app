package com.termux.app.terminal;

import com.termux.app.terminal.session.TransientCommandSessionName;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HostTmuxSessionKillerTest {

    private static final String KILL_TEMPLATE = "ssh host tmux kill-session -t {name}";

    private static final class RecordingTarget implements HostTmuxSessionKiller.Target {
        final List<String> events = new ArrayList<>();
        String executedCommandSessionName;
        String executedHostKillCommand;
        boolean executionSucceeds = true;

        @Override
        public boolean executeHostKillCommand(String commandSessionName, String hostKillCommand) {
            executedCommandSessionName = commandSessionName;
            executedHostKillCommand = hostKillCommand;
            events.add("execute");
            return executionSucceeds;
        }

        @Override
        public void notifyUnavailable(KillHostSessionPlan.Outcome outcome) {
            events.add("notify-" + outcome);
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
    public void executesTheHostKillCommandOutOfBandBeforeTearingDownTheLocalSessionRow() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE, "host-session", scheduler);

        Assert.assertEquals("the host kill command must be dispatched before any local teardown is scheduled",
            List.of("execute"), target.events);
        scheduler.runScheduledTask();
        Assert.assertEquals("the local session row must be torn down only after the host command was dispatched",
            List.of("execute", "finish"), target.events);
    }

    @Test
    public void dispatchesTheStoredTemplateWithTheNormalizedSessionNameSubstituted() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE,
            "https://github.com/owner/repo/issues/123", scheduler);

        Assert.assertEquals("ssh host tmux kill-session -t 'https_//github_com/owner/repo/issues/123'",
            target.executedHostKillCommand);
    }

    @Test
    public void dispatchesUnderATransientCommandSessionNameDerivedFromTheHostSession() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE, "host-session", scheduler);

        Assert.assertEquals("the command session must be named after the host session it kills, so a second "
                + "kill collides with the running one instead of starting another identical session",
            TransientCommandSessionName.forKillOfSession("host-session"), target.executedCommandSessionName);
        Assert.assertTrue(TransientCommandSessionName.isTransient(target.executedCommandSessionName));
    }

    @Test
    public void doesNotFinishLocalSessionUntilSchedulerFires() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE, "host-session", scheduler);

        Assert.assertFalse("teardown must be deferred, not run synchronously after the dispatch",
            target.events.contains("finish"));
        Assert.assertEquals(HostTmuxSessionKiller.HOST_KILL_TRANSIT_GRACE_MILLIS, scheduler.scheduledDelayMillis);
    }

    @Test
    public void keepsTheSessionRowWhenTheHostCommandCouldNotBeDispatched() {
        RecordingTarget target = new RecordingTarget();
        target.executionSucceeds = false;
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE, "host-session", scheduler);

        Assert.assertEquals("a refused dispatch must not look like a successful host kill",
            List.of("execute"), target.events);
        Assert.assertNull(scheduler.scheduledTask);
    }

    @Test
    public void surfacesNotConfiguredAndKeepsTheSessionRowWhenTemplateIsEmpty() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "", "host-session", scheduler);

        Assert.assertEquals("an unconfigured template must not look like a successful host kill",
            List.of("notify-" + KillHostSessionPlan.Outcome.COMMAND_NOT_CONFIGURED), target.events);
        Assert.assertNull(target.executedHostKillCommand);
        Assert.assertNull(scheduler.scheduledTask);
    }

    @Test
    public void surfacesNotConfiguredAndKeepsTheSessionRowWhenTemplateIsNull() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, null, "host-session", scheduler);

        Assert.assertEquals(List.of("notify-" + KillHostSessionPlan.Outcome.COMMAND_NOT_CONFIGURED), target.events);
        Assert.assertNull(target.executedHostKillCommand);
        Assert.assertNull(scheduler.scheduledTask);
    }

    @Test
    public void surfacesNotConfiguredWhenTemplateIsOnlyWhitespace() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, "   \n", "host-session", scheduler);

        Assert.assertEquals(List.of("notify-" + KillHostSessionPlan.Outcome.COMMAND_NOT_CONFIGURED), target.events);
        Assert.assertNull(target.executedHostKillCommand);
    }

    @Test
    public void reportsTheMissingSessionNameRatherThanClaimingTheTemplateIsNotConfigured() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE, "", scheduler);

        Assert.assertEquals("a user who configured a template must never be told it is not configured",
            List.of("notify-" + KillHostSessionPlan.Outcome.SESSION_NAME_MISSING), target.events);
        Assert.assertNull(target.executedHostKillCommand);
        Assert.assertNull(scheduler.scheduledTask);
    }

    @Test
    public void reportsTheMissingSessionNameForANullSessionNameToo() {
        RecordingTarget target = new RecordingTarget();
        DeferredScheduler scheduler = new DeferredScheduler();

        HostTmuxSessionKiller.kill(target, KILL_TEMPLATE, null, scheduler);

        Assert.assertEquals(List.of("notify-" + KillHostSessionPlan.Outcome.SESSION_NAME_MISSING), target.events);
        Assert.assertNull(scheduler.scheduledTask);
    }
}
