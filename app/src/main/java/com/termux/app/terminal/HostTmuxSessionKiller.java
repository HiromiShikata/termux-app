package com.termux.app.terminal;

public final class HostTmuxSessionKiller {

    public interface Target {
        boolean executeHostKillCommand(String hostKillCommand);

        void notifyKillCommandNotConfigured();

        void notifyHostSessionNameMissing();

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

        if (!HostTmuxSessionKillCommand.hasCommandTemplate(commandTemplate)) {
            target.notifyKillCommandNotConfigured();
            return;
        }

        String hostKillCommand = HostTmuxSessionKillCommand.forSessionName(sessionName, commandTemplate);
        if (hostKillCommand == null) {
            target.notifyHostSessionNameMissing();
            return;
        }

        if (!target.executeHostKillCommand(hostKillCommand)) return;

        scheduler.scheduleAfterDelay(target::finishLocalSession, HOST_KILL_TRANSIT_GRACE_MILLIS);
    }
}
