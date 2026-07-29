package com.termux.app.terminal;

public final class HostTmuxSessionKiller {

    public interface Target {
        boolean executeHostKillCommand(String commandSessionName, String hostKillCommand);

        void notifyUnavailable(KillHostSessionPlan.Outcome outcome);

        void finishLocalSession();
    }

    public interface DelayScheduler {
        void scheduleAfterDelay(Runnable task, long delayMillis);
    }

    public static final long HOST_KILL_TRANSIT_GRACE_MILLIS = 1500L;

    private HostTmuxSessionKiller() {
    }

    public static void kill(Target target, String commandTemplate, String sessionName, DelayScheduler scheduler) {
        if (target == null) return;

        KillHostSessionPlan plan = KillHostSessionPlanner.plan(commandTemplate, sessionName);
        if (!plan.shouldStart()) {
            target.notifyUnavailable(plan.getOutcome());
            return;
        }

        if (!target.executeHostKillCommand(plan.getSessionName(), plan.getCommand())) return;

        scheduler.scheduleAfterDelay(target::finishLocalSession, HOST_KILL_TRANSIT_GRACE_MILLIS);
    }
}
