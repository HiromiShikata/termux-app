package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SessionListDuplicateStableIdTest {

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static final String NA = "N/A";

    @Test
    public void twoSessionRowsSharingANameCollideWhenIdentifiedOnlyByThatNonUniqueName() {
        long firstNamedRowId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.session(3, "worker"));
        long secondNamedRowId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.session(4, "worker"));

        Assert.assertEquals(firstNamedRowId, secondNamedRowId);
    }

    @Test
    public void twoSessionRowsSharingANameButCarryingDistinctHandlesReceiveDistinctStableIds() {
        long firstRowId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.session(3, "worker", "handle-a"));
        long secondRowId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.session(4, "worker", "handle-b"));

        Assert.assertNotEquals(firstRowId, secondRowId);
    }

    @Test
    public void theSameSessionKeepsItsStableIdAcrossReordersWhenIdentifiedByItsHandle() {
        long beforeReorderId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.session(0, "worker", "handle-a"));
        long afterReorderId = TermuxSessionsListViewController.rowItemId(
            SessionHierarchyRow.session(5, "worker", "handle-a"));

        Assert.assertEquals(beforeReorderId, afterReorderId);
    }

    @Test
    public void diffUtilIdentityDistinguishesSessionsThatShareANameButHaveDistinctHandles() {
        Assert.assertFalse(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.session(3, "worker", "handle-a"),
            SessionHierarchyRow.session(4, "worker", "handle-b")));
        Assert.assertTrue(TermuxSessionsListViewController.sameRowIdentity(
            SessionHierarchyRow.session(0, "worker", "handle-a"),
            SessionHierarchyRow.session(5, "worker", "handle-a")));
    }

    @Test
    public void everyRowInAHierarchyWithProjectHeadersAndCollidingSessionNamesHasAUniqueStableId() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("ALPHA", "Story1",
                Collections.singletonList("https://example.test/a")));
        List<String> sessionNames = Arrays.asList(
            "https://example.test/a", "worker", "worker", "worker");
        List<String> sessionHandles = Arrays.asList(
            "handle-alpha", "handle-worker-1", "handle-worker-2", "handle-worker-3");

        List<SessionHierarchyRow> rows =
            builder.build(sessionNames, sessionHandles, entries, NA, Collections.emptySet());

        assertNoTwoRowsShareAStableId(rows);
        Assert.assertTrue("at least two session rows sharing the name \"worker\" must be present",
            countSessionRowsWithName(rows, sessionNames, "worker") >= 2);
    }

    @Test
    public void everyRowInAFlatListOfManySameNamedSessionsHasAUniqueStableId() {
        List<String> sessionNames = new ArrayList<>();
        List<String> sessionHandles = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < 8; sessionIndex++) {
            sessionNames.add("worker");
            sessionHandles.add("handle-" + sessionIndex);
        }

        List<SessionHierarchyRow> rows =
            builder.build(sessionNames, sessionHandles, Collections.emptyList(), NA, Collections.emptySet());

        Assert.assertEquals(8, rows.size());
        assertNoTwoRowsShareAStableId(rows);
    }

    private static void assertNoTwoRowsShareAStableId(List<SessionHierarchyRow> rows) {
        Set<Long> seenStableIds = new HashSet<>();
        for (SessionHierarchyRow row : rows) {
            long stableId = TermuxSessionsListViewController.rowItemId(row);
            Assert.assertTrue(
                "duplicate stable id " + stableId + " for row " + describeRow(row),
                seenStableIds.add(stableId));
        }
    }

    private static int countSessionRowsWithName(List<SessionHierarchyRow> rows,
                                                List<String> sessionNames, String targetName) {
        int matchingRowCount = 0;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() != SessionHierarchyRow.Type.SESSION) {
                continue;
            }
            int sessionIndex = row.getSessionIndex();
            if (sessionIndex >= 0 && sessionIndex < sessionNames.size()
                && targetName.equals(sessionNames.get(sessionIndex))) {
                matchingRowCount++;
            }
        }
        return matchingRowCount;
    }

    private static String describeRow(SessionHierarchyRow row) {
        if (row.getType() == SessionHierarchyRow.Type.SESSION) {
            return "session#" + row.getSessionIndex() + " name=" + row.getSessionName()
                + " handle=" + row.getSessionHandle();
        }
        return row.getType() + "|" + row.getLabel();
    }
}
