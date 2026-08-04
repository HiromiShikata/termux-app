package com.termux.shared.termux.settings.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UserRemovedSessionTimeSerializer {

    private static final String LOG_TAG = "UserRemovedSessionTimeSerializer";

    private static final char REMOVAL_TIME_SEPARATOR = ' ';

    private UserRemovedSessionTimeSerializer() {
    }

    @NonNull
    public static Map<String, Long> parse(@Nullable String value) {
        Map<String, Long> removedAtMillisBySessionName = new LinkedHashMap<>();
        if (value == null) {
            return removedAtMillisBySessionName;
        }
        for (String line : value.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            int separatorIndex = trimmedLine.indexOf(REMOVAL_TIME_SEPARATOR);
            String sessionName = separatorIndex < 0
                ? "" : trimmedLine.substring(separatorIndex + 1).trim();
            if (separatorIndex <= 0 || sessionName.isEmpty()) {
                Logger.logError(LOG_TAG, "Discarding a stored removal record without a removal time"
                    + " and a session name: " + trimmedLine);
                continue;
            }
            String removalTime = trimmedLine.substring(0, separatorIndex);
            try {
                removedAtMillisBySessionName.put(sessionName, Long.valueOf(removalTime));
            } catch (NumberFormatException removalTimeIsNotANumber) {
                Logger.logError(LOG_TAG, "Discarding a stored removal record whose removal time is not"
                    + " a number: " + trimmedLine);
            }
        }
        return removedAtMillisBySessionName;
    }

    @NonNull
    public static String serialize(@NonNull Map<String, Long> removedAtMillisBySessionName) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Long> removalRecord : removedAtMillisBySessionName.entrySet()) {
            String sessionName = removalRecord.getKey();
            Long removedAtMillis = removalRecord.getValue();
            if (sessionName == null || sessionName.trim().isEmpty() || removedAtMillis == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(removedAtMillis).append(REMOVAL_TIME_SEPARATOR).append(sessionName.trim());
        }
        return builder.toString();
    }
}
