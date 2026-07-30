package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HiddenSessionNameKeepsATappableRowTest {

    private static final String NA_PROJECT_LABEL = "N/A";
    private static final String PROJECT_LABEL = "umino";
    private static final String PROJECT_MANAGER_SESSION_NAME = "uminopm";
    private static final String STORY_LABEL = "story";
    private static final String STORY_SESSION_URL = "https://example.test/story-session";
    private static final String ALWAYS_NOT_APPLICABLE_SESSION_NAME = "agent-inbox";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    @Test
    public void aHiddenAlwaysNotApplicableNameKeepsARowWithNoLiveSessionIndexWhileEveryEntryIsLoaded() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(STORY_SESSION_URL), storyEntries(), NA_PROJECT_LABEL,
            namesInOrder(ALWAYS_NOT_APPLICABLE_SESSION_NAME),
            namesInOrder(ALWAYS_NOT_APPLICABLE_SESSION_NAME));

        SessionHierarchyRow rowForTheHiddenName =
            sessionRowForName(rows, ALWAYS_NOT_APPLICABLE_SESSION_NAME);
        assertNotNull("a name the owner hid must still be drawn as a row he can tap to unhide, and an "
                + "always-not-applicable name is never an entry of the session definition document, so the row "
                + "must appear even though every entry is loaded and none of them names it",
            rowForTheHiddenName);
        assertEquals("the row for a hidden name must carry no live session index, because hiding released the "
                + "session and removed it from the service",
            SessionHierarchyBuilder.NO_LIVE_SESSION_INDEX, rowForTheHiddenName.getSessionIndex());
    }

    @Test
    public void aHiddenProjectManagerNameKeepsARowWithNoLiveSessionIndexWhileEveryEntryIsLoaded() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(STORY_SESSION_URL), storyEntries(), NA_PROJECT_LABEL,
            Collections.emptySet(), namesInOrder(PROJECT_MANAGER_SESSION_NAME));

        SessionHierarchyRow rowForTheHiddenName = sessionRowForName(rows, PROJECT_MANAGER_SESSION_NAME);
        assertNotNull("a project manager name the owner hid must still be drawn as a row he can tap to unhide, "
                + "and a project manager name is derived from a project label rather than listed as an entry "
                + "url, so the row must appear even though every entry is loaded",
            rowForTheHiddenName);
        assertEquals("the row for a hidden project manager name must carry no live session index",
            SessionHierarchyBuilder.NO_LIVE_SESSION_INDEX, rowForTheHiddenName.getSessionIndex());
    }

    @Test
    public void aHiddenNameKeepsARowWhileTheEntryListIsEmpty() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(STORY_SESSION_URL), Collections.emptyList(), NA_PROJECT_LABEL,
            Collections.emptySet(), namesInOrder(ALWAYS_NOT_APPLICABLE_SESSION_NAME));

        SessionHierarchyRow rowForTheHiddenName =
            sessionRowForName(rows, ALWAYS_NOT_APPLICABLE_SESSION_NAME);
        assertNotNull("a name the owner hid must still be drawn as a row he can tap to unhide while the session "
                + "definition document is unavailable, because the entry list is then the only other row source",
            rowForTheHiddenName);
        assertEquals("the row for a hidden name must carry no live session index while the entry list is empty",
            SessionHierarchyBuilder.NO_LIVE_SESSION_INDEX, rowForTheHiddenName.getSessionIndex());
    }

    @Test
    public void aHiddenNameThatStillHasALiveSessionKeepsExactlyOneRow() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(STORY_SESSION_URL), storyEntries(), NA_PROJECT_LABEL,
            Collections.emptySet(), namesInOrder(STORY_SESSION_URL));

        assertEquals("a hidden name whose session is still live already has a row, so the hidden row source "
                + "must not draw a second row for it: " + sessionRowNames(rows),
            1, sessionRowNames(rows).size());
    }

    @Test
    public void drawingARowForAHiddenNameAddsNoLiveSessionIndexToNavigateTo() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(STORY_SESSION_URL), storyEntries(), NA_PROJECT_LABEL,
            Collections.emptySet(), namesInOrder(ALWAYS_NOT_APPLICABLE_SESSION_NAME));

        assertEquals("the row drawn for a hidden name must add no navigable session index, because the session "
                + "must stay released with no shell process and no terminal emulator",
            Collections.singletonList(0), SessionHierarchyBuilder.visibleSessionIndexes(rows));
    }

    @Test
    public void theRowDrawnForAHiddenNameIsCountedInTheDisplayedSessionCountAndInItsGroupCount() {
        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(STORY_SESSION_URL), storyEntries(), NA_PROJECT_LABEL,
            Collections.emptySet(), namesInOrder(ALWAYS_NOT_APPLICABLE_SESSION_NAME));

        assertEquals("the row drawn for a hidden name is a session row, so the displayed session count "
                + "shown in the session list title counts it alongside the live session, exactly as the "
                + "count already treats a row for a definition-backed name with no live session; the "
                + "rows were " + sessionRowNames(rows),
            2, SessionHierarchyBuilder.totalSessionCount(rows));
        assertEquals("the row drawn for a hidden name is placed in the not-applicable group, so the "
                + "count rendered on that group header counts it",
            Integer.valueOf(1),
            SessionHierarchyBuilder.sessionCountByProjectLabel(rows).get(NA_PROJECT_LABEL));
        assertEquals("drawing a row for a hidden name must not change the count rendered on the header "
                + "of any other group",
            Integer.valueOf(1),
            SessionHierarchyBuilder.sessionCountByProjectLabel(rows).get(PROJECT_LABEL));
    }

    private static List<SessionDefinitionEntry> storyEntries() {
        return Collections.singletonList(
            new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
                Collections.singletonList(STORY_SESSION_URL)));
    }

    private static SessionHierarchyRow sessionRowForName(List<SessionHierarchyRow> rows, String sessionName) {
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader() && sessionName.equals(row.getSessionName())) {
                return row;
            }
        }
        return null;
    }

    private static List<String> sessionRowNames(List<SessionHierarchyRow> rows) {
        List<String> sessionRowNames = new ArrayList<>();
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader()) {
                sessionRowNames.add(row.getSessionName());
            }
        }
        return sessionRowNames;
    }

    private static Set<String> namesInOrder(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }
}
