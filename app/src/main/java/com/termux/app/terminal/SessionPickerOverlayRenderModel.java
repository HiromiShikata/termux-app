package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.app.browser.BrowserGithubUrlShortener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SessionPickerOverlayRenderModel {

    static final int SECONDARY_MAX_CHARACTERS = 42;

    private static final char ELLIPSIS = '…';

    private SessionPickerOverlayRenderModel() {
    }

    @NonNull
    public static List<SessionPickerOverlayLine> build(@NonNull List<SessionHierarchyRow> visibleRows,
                                                       @NonNull List<String> sessionRawNames,
                                                       @NonNull List<String> sessionTitles,
                                                       @NonNull Set<Integer> markedSessionIndexes,
                                                       @NonNull Set<Integer> disabledSessionIndexes,
                                                       int highlightedSessionIndex) {
        List<SessionHierarchyRow> renderableRows =
            renderableRowsExcludingDisabledSessions(visibleRows, disabledSessionIndexes);
        List<SessionPickerOverlayLine> lines = new ArrayList<>(renderableRows.size());
        for (SessionHierarchyRow row : renderableRows) {
            switch (row.getType()) {
                case PROJECT_HEADER:
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.PROJECT, labelOrEmpty(row.getLabel()), false));
                    break;
                case STORY_HEADER:
                    if (!lines.isEmpty()) {
                        lines.add(new SessionPickerOverlayLine(
                            SessionPickerOverlayLine.Kind.SPACER, "", false));
                    }
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.STORY, labelOrEmpty(row.getLabel()), false));
                    break;
                case SESSION:
                default:
                    int sessionIndex = row.getSessionIndex();
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.SESSION,
                        sessionPrimaryName(sessionRawNames, sessionIndex),
                        truncateSecondaryToSingleLine(sessionSecondaryTitle(sessionTitles, sessionIndex)),
                        sessionIndex == highlightedSessionIndex,
                        markedSessionIndexes.contains(sessionIndex)));
                    break;
            }
        }
        return lines;
    }

    @NonNull
    private static List<SessionHierarchyRow> renderableRowsExcludingDisabledSessions(
        @NonNull List<SessionHierarchyRow> visibleRows, @NonNull Set<Integer> disabledSessionIndexes) {
        List<SessionHierarchyRow> sessionFilteredRows = new ArrayList<>(visibleRows.size());
        for (SessionHierarchyRow row : visibleRows) {
            if (row.getType() == SessionHierarchyRow.Type.SESSION
                && disabledSessionIndexes.contains(row.getSessionIndex())) {
                continue;
            }
            sessionFilteredRows.add(row);
        }
        List<SessionHierarchyRow> renderableRows = new ArrayList<>(sessionFilteredRows.size());
        for (int rowIndex = 0; rowIndex < sessionFilteredRows.size(); rowIndex++) {
            SessionHierarchyRow row = sessionFilteredRows.get(rowIndex);
            if (row.getType() == SessionHierarchyRow.Type.STORY_HEADER
                && !hasSessionBeforeNextHeader(sessionFilteredRows, rowIndex)) {
                continue;
            }
            renderableRows.add(row);
        }
        return renderableRows;
    }

    private static boolean hasSessionBeforeNextHeader(@NonNull List<SessionHierarchyRow> rows, int headerIndex) {
        for (int rowIndex = headerIndex + 1; rowIndex < rows.size(); rowIndex++) {
            SessionHierarchyRow row = rows.get(rowIndex);
            if (row.getType() == SessionHierarchyRow.Type.SESSION) {
                return true;
            }
            if (row.isHeader()) {
                return false;
            }
        }
        return false;
    }

    @NonNull
    static String truncateSecondaryToSingleLine(@NonNull String secondaryText) {
        String singleLine = secondaryText.replace('\n', ' ').replace('\r', ' ').trim();
        if (singleLine.length() <= SECONDARY_MAX_CHARACTERS) {
            return singleLine;
        }
        return singleLine.substring(0, SECONDARY_MAX_CHARACTERS - 1).trim() + ELLIPSIS;
    }

    @NonNull
    private static String labelOrEmpty(String label) {
        return label == null ? "" : label;
    }

    @NonNull
    private static String sessionPrimaryName(@NonNull List<String> sessionRawNames, int sessionIndex) {
        if (sessionIndex < 0 || sessionIndex >= sessionRawNames.size()) {
            return "session " + sessionIndex;
        }
        String name = sessionRawNames.get(sessionIndex);
        if (name == null || name.isEmpty()) {
            return "session " + sessionIndex;
        }
        return BrowserGithubUrlShortener.shorten(name);
    }

    @NonNull
    private static String sessionSecondaryTitle(@NonNull List<String> sessionTitles, int sessionIndex) {
        if (sessionIndex < 0 || sessionIndex >= sessionTitles.size()) {
            return "";
        }
        String title = sessionTitles.get(sessionIndex);
        return title == null ? "" : title;
    }
}
