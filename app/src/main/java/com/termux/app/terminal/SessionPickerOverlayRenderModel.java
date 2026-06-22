package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.app.browser.BrowserGithubUrlShortener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SessionPickerOverlayRenderModel {

    static final int SECONDARY_MAX_CHARACTERS = 42;

    private static final char ELLIPSIS = '…';

    private SessionPickerOverlayRenderModel() {
    }

    @NonNull
    public static List<SessionPickerOverlayLine> build(@NonNull List<SessionHierarchyRow> visibleRows,
                                                       @NonNull Map<Integer, SessionRow> sessionRowsByIndex,
                                                       int highlightedSessionIndex) {
        List<SessionHierarchyRow> renderableRows =
            renderableRowsExcludingDisabledSessions(visibleRows, sessionRowsByIndex);
        List<SessionPickerOverlayLine> lines = new ArrayList<>(renderableRows.size());
        for (SessionHierarchyRow row : renderableRows) {
            switch (row.getType()) {
                case PROJECT_HEADER:
                    if (!lines.isEmpty()) {
                        lines.add(new SessionPickerOverlayLine(
                            SessionPickerOverlayLine.Kind.SPACER, "", false));
                    }
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.PROJECT, labelOrEmpty(row.getLabel()), false));
                    break;
                case STORY_HEADER:
                    if (lastLineKind(lines) == SessionPickerOverlayLine.Kind.SESSION) {
                        lines.add(new SessionPickerOverlayLine(
                            SessionPickerOverlayLine.Kind.SPACER, "", false));
                    }
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.STORY, labelOrEmpty(row.getLabel()), false));
                    break;
                case SESSION:
                default:
                    int sessionIndex = row.getSessionIndex();
                    SessionRow sessionRow = SessionRow.rowOrEmpty(sessionRowsByIndex, sessionIndex);
                    boolean isCurrentSession = sessionRow.isCurrent();
                    SessionNewActivityTier tier = sessionRow.getTier();
                    boolean isActivityMarked = sessionRow.isActivityMarked();
                    lines.add(new SessionPickerOverlayLine(
                        SessionPickerOverlayLine.Kind.SESSION,
                        sessionPrimaryName(sessionRow),
                        truncateSecondaryToSingleLine(sessionRow.getResolvedTitle()),
                        sessionIndex == highlightedSessionIndex,
                        isCurrentSession,
                        tier,
                        isActivityMarked ? sessionRow.getActivityAgeLabel() : ""));
                    break;
            }
        }
        return lines;
    }

    @NonNull
    private static List<SessionHierarchyRow> renderableRowsExcludingDisabledSessions(
        @NonNull List<SessionHierarchyRow> visibleRows, @NonNull Map<Integer, SessionRow> sessionRowsByIndex) {
        List<SessionHierarchyRow> sessionFilteredRows = new ArrayList<>(visibleRows.size());
        for (SessionHierarchyRow row : visibleRows) {
            if (row.getType() == SessionHierarchyRow.Type.SESSION
                && SessionRow.rowOrEmpty(sessionRowsByIndex, row.getSessionIndex()).isDisabled()) {
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

    private static SessionPickerOverlayLine.Kind lastLineKind(@NonNull List<SessionPickerOverlayLine> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        return lines.get(lines.size() - 1).getKind();
    }

    @NonNull
    private static String labelOrEmpty(String label) {
        return label == null ? "" : label;
    }

    @NonNull
    private static String sessionPrimaryName(@NonNull SessionRow sessionRow) {
        String name = sessionRow.getName();
        if (name.isEmpty()) {
            return "session " + sessionRow.getSessionIndex();
        }
        return BrowserGithubUrlShortener.shorten(name);
    }
}
