package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SessionInfoBlock {

    private final String text;
    private final Map<SessionInfoLine, Integer> startOffsets;
    private final Map<SessionInfoLine, Integer> endOffsets;
    private final List<SessionInfoLine> orderedNonEmptyLines;

    private SessionInfoBlock(@NonNull String text,
                             @NonNull Map<SessionInfoLine, Integer> startOffsets,
                             @NonNull Map<SessionInfoLine, Integer> endOffsets,
                             @NonNull List<SessionInfoLine> orderedNonEmptyLines) {
        this.text = text;
        this.startOffsets = startOffsets;
        this.endOffsets = endOffsets;
        this.orderedNonEmptyLines = orderedNonEmptyLines;
    }

    @NonNull
    static SessionInfoBlock compose(@NonNull String bellNotificationLabel,
                                    @NonNull String sessionName,
                                    @NonNull String timestamp,
                                    @NonNull String definitionTitle,
                                    @NonNull String sessionTitle,
                                    @NonNull String explicitCallReason) {
        Map<SessionInfoLine, String> partsByLine = new EnumMap<>(SessionInfoLine.class);
        partsByLine.put(SessionInfoLine.BELL_NOTIFICATION_LABEL, bellNotificationLabel);
        partsByLine.put(SessionInfoLine.SESSION_NAME, sessionName);
        partsByLine.put(SessionInfoLine.TIMESTAMP, timestamp);
        partsByLine.put(SessionInfoLine.DEFINITION_TITLE, definitionTitle);
        partsByLine.put(SessionInfoLine.SESSION_TITLE, sessionTitle);
        partsByLine.put(SessionInfoLine.EXPLICIT_CALL_REASON, explicitCallReason);

        StringBuilder builder = new StringBuilder();
        Map<SessionInfoLine, Integer> startOffsets = new EnumMap<>(SessionInfoLine.class);
        Map<SessionInfoLine, Integer> endOffsets = new EnumMap<>(SessionInfoLine.class);
        List<SessionInfoLine> orderedNonEmptyLines = new java.util.ArrayList<>();
        boolean hasPrecedingLine = false;
        for (SessionInfoLine line : SessionInfoLine.values()) {
            String part = partsByLine.get(line);
            if (part == null || part.isEmpty()) {
                startOffsets.put(line, -1);
                endOffsets.put(line, -1);
                continue;
            }
            if (hasPrecedingLine) {
                builder.append("\n");
            }
            int start = builder.length();
            builder.append(part);
            startOffsets.put(line, start);
            endOffsets.put(line, builder.length());
            orderedNonEmptyLines.add(line);
            hasPrecedingLine = true;
        }
        return new SessionInfoBlock(builder.toString(), startOffsets, endOffsets, orderedNonEmptyLines);
    }

    @NonNull
    String text() {
        return text;
    }

    int startOf(@NonNull SessionInfoLine line) {
        Integer start = startOffsets.get(line);
        return start == null ? -1 : start;
    }

    int endOf(@NonNull SessionInfoLine line) {
        Integer end = endOffsets.get(line);
        return end == null ? -1 : end;
    }

    @NonNull
    List<SessionInfoLine> orderedNonEmptyLines() {
        return java.util.Collections.unmodifiableList(orderedNonEmptyLines);
    }
}
