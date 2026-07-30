package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A project-manager session name is derived from a project label, so the session definition entries
 * do not have to carry it. While the derived name had no live session and appeared in no entry, the
 * session list drew no row for it at all, and turning the list filter off could not bring the row
 * back because the filter only ever removes rows. Every project the session definition names now
 * draws exactly one project-manager row, first inside its own project group, whether or not a live
 * session exists for it.
 */
public class SessionListDrawsAProjectManagerRowForEveryProjectTest {

    private static final String NA = "N/A";
    private static final String FIRST_PROJECT_LABEL = "projectone";
    private static final String SECOND_PROJECT_LABEL = "projecttwo";
    private static final String FIRST_PROJECT_MANAGER_SESSION_NAME = FIRST_PROJECT_LABEL
        + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    private static final String SECOND_PROJECT_MANAGER_SESSION_NAME = SECOND_PROJECT_LABEL
        + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    private static final String FIRST_STORY_LABEL = "storyone";
    private static final String SECOND_STORY_LABEL = "storytwo";
    private static final String FIRST_STORY_SESSION_NAME = "https://example.test/story-one";
    private static final String SECOND_STORY_SESSION_NAME = "https://example.test/story-two";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static SessionDefinitionEntry firstProjectEntry() {
        return new SessionDefinitionEntry(FIRST_PROJECT_LABEL, FIRST_STORY_LABEL,
            Collections.singletonList(FIRST_STORY_SESSION_NAME));
    }

    private static SessionDefinitionEntry secondProjectEntry() {
        return new SessionDefinitionEntry(SECOND_PROJECT_LABEL, SECOND_STORY_LABEL,
            Collections.singletonList(SECOND_STORY_SESSION_NAME));
    }

    private static List<SessionDefinitionEntry> definitionNamingOnlyTheFirstProject() {
        return Collections.singletonList(firstProjectEntry());
    }

    private static List<SessionDefinitionEntry> definitionNamingBothProjects() {
        return Arrays.asList(firstProjectEntry(), secondProjectEntry());
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

    private static int indexOfProjectHeader(List<SessionHierarchyRow> rows, String label) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            SessionHierarchyRow row = rows.get(rowIndex);
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER
                    && label.equals(row.getLabel())) {
                return rowIndex;
            }
        }
        return -1;
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

    private static SessionHierarchyRow firstRowInsideProject(List<SessionHierarchyRow> rows,
                                                             String projectLabel) {
        int projectHeaderIndex = indexOfProjectHeader(rows, projectLabel);
        Assert.assertTrue("the project group must have a header. Actual:\n" + dump(rows),
            projectHeaderIndex >= 0 && projectHeaderIndex + 1 < rows.size());
        return rows.get(projectHeaderIndex + 1);
    }

    @Test
    public void aProjectWhoseProjectManagerSessionIsNotLiveStillDrawsOneProjectManagerRowFirstInItsGroup() {
        List<String> liveSessionNames = Collections.singletonList(FIRST_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionNamingOnlyTheFirstProject(), NA);

        SessionHierarchyRow firstRowInsideTheProject = firstRowInsideProject(rows, FIRST_PROJECT_LABEL);
        Assert.assertEquals("a project-manager name is derived from the project label, so the session"
                + " definition entries need not carry it; the project-manager row must still be the"
                + " first row inside its project group while no live session exists for it. Actual:\n"
                + dump(rows),
            FIRST_PROJECT_MANAGER_SESSION_NAME, firstRowInsideTheProject.getSessionName());
        Assert.assertEquals("a project-manager row drawn without a live session must carry the"
                + " no-live-session index so it is rendered as a definition-backed row. Actual:\n"
                + dump(rows),
            SessionHierarchyBuilder.NO_LIVE_SESSION_INDEX, firstRowInsideTheProject.getSessionIndex());
        Assert.assertEquals("a project must draw exactly one project-manager row. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
        Assert.assertEquals("the project-manager row must not be placed under the not-applicable"
            + " header. Actual:\n" + dump(rows), -1, indexOfProjectHeader(rows, NA));
    }

    @Test
    public void everyProjectInTheDefinitionDrawsItsOwnProjectManagerRow() {
        List<SessionHierarchyRow> rows =
            builder.build(Collections.emptyList(), definitionNamingBothProjects(), NA);

        Assert.assertEquals("the first project must draw its project-manager row. Actual:\n" + dump(rows),
            FIRST_PROJECT_MANAGER_SESSION_NAME,
            firstRowInsideProject(rows, FIRST_PROJECT_LABEL).getSessionName());
        Assert.assertEquals("the second project must draw its project-manager row. Actual:\n" + dump(rows),
            SECOND_PROJECT_MANAGER_SESSION_NAME,
            firstRowInsideProject(rows, SECOND_PROJECT_LABEL).getSessionName());
    }

    @Test
    public void aProjectManagerRowWhoseNameIsHiddenIsDrawnWithTheListFilterOff() {
        List<String> liveSessionNames = Collections.singletonList(FIRST_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows = SessionHierarchyBuilder.filterHiddenSessions(
            builder.build(liveSessionNames, definitionNamingOnlyTheFirstProject(), NA),
            liveSessionNames, Collections.emptySet());

        Assert.assertEquals("with the list filter off no name is hidden, so a project-manager row whose"
                + " name sits in the hidden-name set must still be drawn. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aProjectManagerRowWhoseNameIsHiddenIsRemovedWithTheListFilterOn() {
        List<String> liveSessionNames = Collections.singletonList(FIRST_STORY_SESSION_NAME);
        Set<String> hiddenSessionNames =
            Collections.singleton(FIRST_PROJECT_MANAGER_SESSION_NAME);
        List<SessionHierarchyRow> unfilteredRows =
            builder.build(liveSessionNames, definitionNamingOnlyTheFirstProject(), NA);
        Assert.assertEquals("the arrangement must draw the project-manager row before the assertion"
                + " below means anything. Actual:\n" + dump(unfilteredRows),
            1, rowCountForSessionName(unfilteredRows, FIRST_PROJECT_MANAGER_SESSION_NAME));

        List<SessionHierarchyRow> rows = SessionHierarchyBuilder.filterHiddenSessions(
            unfilteredRows, liveSessionNames, hiddenSessionNames);

        Assert.assertEquals("a hidden project-manager row must behave like every other hidden row and be"
                + " removed with the list filter on. Actual:\n" + dump(rows),
            0, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void aProjectWhoseProjectManagerSessionIsLiveDrawsExactlyOneProjectManagerRow() {
        List<String> liveSessionNames =
            Arrays.asList(FIRST_STORY_SESSION_NAME, FIRST_PROJECT_MANAGER_SESSION_NAME);

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionNamingOnlyTheFirstProject(), NA);

        Assert.assertEquals("a live project-manager session and the derived project-manager name are the"
                + " same session, so exactly one row must be drawn for them. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
        SessionHierarchyRow firstRowInsideTheProject = firstRowInsideProject(rows, FIRST_PROJECT_LABEL);
        Assert.assertEquals("the live project-manager session must stay the first row inside its project"
                + " group. Actual:\n" + dump(rows),
            FIRST_PROJECT_MANAGER_SESSION_NAME, firstRowInsideTheProject.getSessionName());
        Assert.assertEquals("the project-manager row must carry the live session index so tapping it"
                + " switches to the running session. Actual:\n" + dump(rows),
            liveSessionNames.indexOf(FIRST_PROJECT_MANAGER_SESSION_NAME),
            firstRowInsideTheProject.getSessionIndex());
    }

    @Test
    public void aProjectThatLeftTheSessionDefinitionDrawsNoProjectManagerRow() {
        List<SessionHierarchyRow> rowsWhileBothProjectsAreNamed =
            builder.build(Collections.emptyList(), definitionNamingBothProjects(), NA);
        Assert.assertEquals("the arrangement must draw the second project's project-manager row before"
                + " the assertion below means anything. Actual:\n" + dump(rowsWhileBothProjectsAreNamed),
            1, rowCountForSessionName(rowsWhileBothProjectsAreNamed, SECOND_PROJECT_MANAGER_SESSION_NAME));

        List<SessionHierarchyRow> rowsAfterTheSecondProjectLeft =
            builder.build(Collections.emptyList(), definitionNamingOnlyTheFirstProject(), NA);

        Assert.assertEquals("only the projects the session definition currently names may draw a"
                + " project-manager row, so a project that left must draw none. Actual:\n"
                + dump(rowsAfterTheSecondProjectLeft),
            0, rowCountForSessionName(rowsAfterTheSecondProjectLeft, SECOND_PROJECT_MANAGER_SESSION_NAME));
        Assert.assertEquals("a project that left the session definition must draw no project header"
            + " either. Actual:\n" + dump(rowsAfterTheSecondProjectLeft),
            -1, indexOfProjectHeader(rowsAfterTheSecondProjectLeft, SECOND_PROJECT_LABEL));
    }

    @Test
    public void everyDrawnRowNamesEitherALiveSessionOrANameTheDefinitionCarries() {
        String liveSessionOutsideTheDefinition = "https://example.test/outside-the-definition";
        List<String> liveSessionNames = Arrays.asList(FIRST_STORY_SESSION_NAME,
            SECOND_STORY_SESSION_NAME, liveSessionOutsideTheDefinition);
        Set<String> namesTheDefinitionCarries = new LinkedHashSet<>(Arrays.asList(
            FIRST_STORY_SESSION_NAME, SECOND_STORY_SESSION_NAME,
            FIRST_PROJECT_MANAGER_SESSION_NAME, SECOND_PROJECT_MANAGER_SESSION_NAME));

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionNamingBothProjects(), NA);

        List<String> rowsNamingNeitherALiveSessionNorADefinedName = new ArrayList<>();
        for (SessionHierarchyRow row : rows) {
            if (row.isHeader()) continue;
            int sessionIndex = row.getSessionIndex();
            if (sessionIndex >= 0 && sessionIndex < liveSessionNames.size()) continue;
            if (row.getSessionName() != null
                    && namesTheDefinitionCarries.contains(row.getSessionName())) continue;
            rowsNamingNeitherALiveSessionNorADefinedName.add(
                "index=" + sessionIndex + " name=" + row.getSessionName());
        }
        Assert.assertEquals("no row may be drawn for a session name that is neither a live session, a"
                + " name the session definition lists, nor a project-manager name derived from a project"
                + " label the session definition names. Offending rows: "
                + rowsNamingNeitherALiveSessionNorADefinedName + "\nActual:\n" + dump(rows),
            Collections.emptyList(), rowsNamingNeitherALiveSessionNorADefinedName);
    }

    @Test
    public void aProjectManagerNameTheOwnerAlwaysWantsNotApplicableDrawsNoRowInsideItsProjectGroup() {
        List<String> liveSessionNames =
            Arrays.asList(FIRST_STORY_SESSION_NAME, FIRST_PROJECT_MANAGER_SESSION_NAME);
        Set<String> alwaysNaSessionNames =
            Collections.singleton(FIRST_PROJECT_MANAGER_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(liveSessionNames,
            definitionNamingOnlyTheFirstProject(), NA, alwaysNaSessionNames);

        Assert.assertEquals("a name the owner pinned to the not-applicable group keeps exactly one row"
                + " there, so no second project-manager row may be drawn for it. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
        Assert.assertNotEquals("a name the owner pinned to the not-applicable group must not be drawn"
                + " inside its project group. Actual:\n" + dump(rows),
            FIRST_PROJECT_MANAGER_SESSION_NAME,
            firstRowInsideProject(rows, FIRST_PROJECT_LABEL).getSessionName());
    }
}
