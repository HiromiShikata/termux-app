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
        return false;
    }

    public void forget(@NonNull String sessionName) {
        mUnableSinceMillisBySessionName.remove(sessionName);
    }
}
