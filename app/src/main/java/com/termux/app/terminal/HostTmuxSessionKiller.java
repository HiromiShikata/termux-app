package com.termux.app.terminal;

public final class HostTmuxSessionKiller {

    public interface Target {
        void writeKillCommand(String killCommand);

        void finishLocalSession();
    }

    public interface DelayScheduler {
        void scheduleAfterDelay(Runnable task, long delayMillis);
    }

    public static final long HOST_KILL_TRANSIT_GRACE_MILLIS = 1500L;

    private HostTmuxSessionKiller() {
    }

    public static void kill(Target target, String sessionName, DelayScheduler scheduler) {
        kill(target, sessionName, HostTmuxSessionKillCommand.DEFAULT_TMUX_PREFIX_KEY, scheduler);
    }

    public static void kill(Target target, String sessionName, char tmuxPrefixKey, DelayScheduler scheduler) {
        if (target == null) return;

        String killCommand = HostTmuxSessionKillCommand.forSessionName(sessionName, tmuxPrefixKey);
        if (killCommand == null) {
            target.finishLocalSession();
            return;
        }

        target.writeKillCommand(killCommand);
        scheduler.scheduleAfterDelay(target::finishLocalSession, HOST_KILL_TRANSIT_GRACE_MILLIS);
    }
}
