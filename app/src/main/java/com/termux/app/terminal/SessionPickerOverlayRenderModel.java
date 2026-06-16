package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class SessionPickerOverlayRenderModel {

    private SessionPickerOverlayRenderModel() {
    }

    @NonNull
    public static List<SessionPickerOverlayLine> build(@NonNull List<SessionHierarchyRow> visibleRows,
                                                       @NonNull List<String> sessionDisplayNames,
                                                       int highlightedSessionIndex) {
        List<SessionPickerOverlayLine> lines = new ArrayList<>(visibleRows.size());
        for (SessionHierarchyRow row : visibleRows) {
            switch (row.getType()) {
                case PROJECT_HEADER:
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.PROJECT, labelOrEmpty(row.getLabel()), false));
                    break;
                case STORY_HEADER:
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.STORY, labelOrEmpty(row.getLabel()), false));
                    break;
                case SESSION:
                default:
                    int sessionIndex = row.getSessionIndex();
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.SESSION,
                        sessionDisplayName(sessionDisplayNames, sessionIndex),
                        sessionIndex == highlightedSessionIndex));
                    break;
            }
        }
        return lines;
    }

    @NonNull
    private static String labelOrEmpty(String label) {
        return label == null ? "" : label;
    }

    @NonNull
    private static String sessionDisplayName(@NonNull List<String> sessionDisplayNames, int sessionIndex) {
        if (sessionIndex < 0 || sessionIndex >= sessionDisplayNames.size()) {
            return "session " + sessionIndex;
        }
        String name = sessionDisplayNames.get(sessionIndex);
        if (name == null || name.isEmpty()) {
            return "session " + sessionIndex;
        }
        return name;
    }
}
