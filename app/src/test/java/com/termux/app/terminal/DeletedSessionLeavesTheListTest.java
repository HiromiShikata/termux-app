package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Deleting a session from the long-press menu ends the session and records the name as removed, but
 * the session list draws one row for every name the session definition carries whether or not a
 * session is live for it, so the row was rebuilt exactly as before and the deletion looked as if it
 * had done nothing.
 */
public class DeletedSessionLeavesTheListTest {

    private static final String NA = "N/A";
    private static final String PROJECT_LABEL = "projectone";
    private static final String PROJECT_MANAGER_SESSION_NAME = PROJECT_LABEL
        + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    private static final String STORY_LABEL = "storyone";
    private static final String DELETED_STORY_SESSION_NAME = "https://example.test/story-one";
    private static final String KEPT_STORY_SESSION_NAME = "https://example.test/story-two";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static List<SessionDefinitionEntry> definition() {
        return Collections.singletonList(new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
            Arrays.asList(DELETED_STORY_SESSION_NAME, KEPT_STORY_SESSION_NAME)));
    }

    private static String dump(List<SessionHierarchyRow> rows) {
        StringBuilder text = new StringBuilder();
        for (SessionHierarchyRow row : rows) {
            text.append(row.getType())
                .append(row.isHeader() ? "|" + row.getLabel()
                    : "|index=" + row.getSessionIndex() + "|name=" + row.getSessionName())
                .append('\n');
        }
        return text.toString();
    }

    private static int rowCountForSessionName(List<SessionHierarchyRow> rows, String sessionName) {
        int rowCount = 0;
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader() && sessionName.equals(row.getSessionName())) {
                rowCount++;
            }
        }
        return rowCount;
    }

    @Test
    public void aDeletedStorySessionNameDrawsNoRowWhileNoSessionIsLiveForIt() {
        Set<String> deletedSessionNames = Collections.singleton(DELETED_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(), definition(), NA,
            Collections.emptySet(), deletedSessionNames);

        Assert.assertEquals("the owner deleted this session, so its row must leave the list; leaving it"
                + " there makes the delete action look as if it did nothing. Actual:\n" + dump(rows),
            0, rowCountForSessionName(rows, DELETED_STORY_SESSION_NAME));
    }

    @Test
    public void deletingOneSessionLeavesEveryOtherDefinedRowInPlace() {
        Set<String> deletedSessionNames = Collections.singleton(DELETED_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(), definition(), NA,
            Collections.emptySet(), deletedSessionNames);

        Assert.assertEquals("deleting one session must not remove the rows of the others. Actual:\n"
                + dump(rows),
            1, rowCountForSessionName(rows, KEPT_STORY_SESSION_NAME));
        Assert.assertEquals("the project-manager row of the project is not the deleted name and must"
                + " stay. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aDeletedProjectManagerSessionNameDrawsNoRowWhileNoSessionIsLiveForIt() {
        Set<String> deletedSessionNames = Collections.singleton(PROJECT_MANAGER_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(), definition(), NA,
            Collections.emptySet(), deletedSessionNames);

        Assert.assertEquals("a project-manager row is drawn from a derived name, and the owner can"
                + " delete that session too, so its row must leave the list as well. Actual:\n"
                + dump(rows),
            0, rowCountForSessionName(rows, PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aDeletedNamePinnedToTheNotApplicableGroupDrawsNoRowWhileNoSessionIsLiveForIt() {
        Set<String> alwaysNaSessionNames = Collections.singleton(DELETED_STORY_SESSION_NAME);
        Set<String> deletedSessionNames = Collections.singleton(DELETED_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(), definition(), NA,
            alwaysNaSessionNames, deletedSessionNames);

        Assert.assertEquals("pinning a name to the not-applicable group decides where its row sits,"
                + " never whether a deleted name keeps a row. Actual:\n" + dump(rows),
            0, rowCountForSessionName(rows, DELETED_STORY_SESSION_NAME));
    }

    @Test
    public void aDeletedNameWhoseSessionIsLiveAgainDrawsItsRow() {
        Set<String> deletedSessionNames = Collections.singleton(DELETED_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(
            Collections.singletonList(DELETED_STORY_SESSION_NAME), definition(), NA,
            Collections.emptySet(), deletedSessionNames);

        Assert.assertEquals("a live session must always be reachable from the list, so a name whose"
                + " session exists again draws its row even while the stale removal record is still"
                + " there. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, DELETED_STORY_SESSION_NAME));
    }
}
