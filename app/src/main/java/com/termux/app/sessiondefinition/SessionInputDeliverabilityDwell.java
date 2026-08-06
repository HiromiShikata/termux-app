package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class SessionInputDeliverabilityDwell {

    public static final long DWELL_MILLIS = 90_000L;

    private final Map<String, Long> mUnableSinceMillisBySessionName = new HashMap<>();

    public boolean hasBeenUnableToReceiveInputLongEnough(@NonNull String sessionName,
                                                         boolean canReceiveInput,
                                                         long nowMillis) {
        if (canReceiveInput) {
            mUnableSinceMillisBySessionName.remove(sessionName);
            return false;
        }
        Long unableSinceMillis = mUnableSinceMillisBySessionName.get(sessionName);
        if (unableSinceMillis == null) {
            mUnableSinceMillisBySessionName.put(sessionName, nowMillis);
            return false;
        }
        return nowMillis - unableSinceMillis >= DWELL_MILLIS;
    }

    public void forget(@NonNull String sessionName) {
        mUnableSinceMillisBySessionName.remove(sessionName);
    }
}
