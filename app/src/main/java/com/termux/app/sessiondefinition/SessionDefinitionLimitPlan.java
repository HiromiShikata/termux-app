package com.termux.app.sessiondefinition;

public final class SessionDefinitionLimitPlan {

    private final int sessionsToCreateCount;
    private final int droppedSessionCount;

    private SessionDefinitionLimitPlan(int sessionsToCreateCount, int droppedSessionCount) {
        this.sessionsToCreateCount = sessionsToCreateCount;
        this.droppedSessionCount = droppedSessionCount;
    }

    public static SessionDefinitionLimitPlan forCapacity(int requestedSessionCount, int liveSessionCount,
                                                         int configuredLimit) {
        int remainingCapacity = Math.max(0, configuredLimit - liveSessionCount);
        int sessionsToCreateCount = Math.min(requestedSessionCount, remainingCapacity);
        int droppedSessionCount = requestedSessionCount - sessionsToCreateCount;
        return new SessionDefinitionLimitPlan(sessionsToCreateCount, droppedSessionCount);
    }

    public int getSessionsToCreateCount() {
        return sessionsToCreateCount;
    }

    public int getDroppedSessionCount() {
        return droppedSessionCount;
    }

    public boolean exceedsLimit() {
        return droppedSessionCount > 0;
    }
}
