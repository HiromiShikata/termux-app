package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.TimeZone;

public final class SessionStatuslineReloadScanner {

    public void repopulateFromCurrentStatuslines(@NonNull SessionNewActivityStore store,
                                                 @NonNull Map<String, String> visibleScreenTextBySessionName,
                                                 long nowMillis,
                                                 @NonNull TimeZone timeZone) {
        for (Map.Entry<String, String> entry : visibleScreenTextBySessionName.entrySet()) {
            String sessionName = entry.getKey();
            if (sessionName == null) {
                continue;
            }
            repopulateFromCurrentStatusline(store, sessionName, entry.getValue(), nowMillis, timeZone);
        }
    }

    public void repopulateFromCurrentStatusline(@NonNull SessionNewActivityStore store,
                                                @NonNull String sessionName,
                                                @Nullable String visibleScreenText,
                                                long nowMillis,
                                                @NonNull TimeZone timeZone) {
        ClaudeStatuslineTimes statuslineTimes =
            ClaudeStatuslineTimes.parse(visibleScreenText, nowMillis, timeZone);
        if (!statuslineTimes.hasAnyToken()) {
            return;
        }
        store.recordStatuslineTimes(sessionName,
            statuslineTimes.getCallTimeMillis(),
            statuslineTimes.getOutTimeMillis(),
            statuslineTimes.getReplyTimeMillis(),
            statuslineTimes.getSubagentCount());
        store.clearReconnecting(sessionName);
    }
}
