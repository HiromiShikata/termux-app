package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessionHierarchyBuilderCollapsedProjectSessionNamesTest {

    private static final String NA = "N/A";

    private static final String CLOSED_PROJECT = "project-the-owner-closed";
    private static final String OPEN_PROJECT = "project-the-owner-left-open";
    private static final String SESSION_UNDER_THE_CLOSED_PROJECT = "https://example.test/closed-1";
    private static final String SESSION_UNDER_THE_OPEN_PROJECT = "https://example.test/open-1";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private List<SessionHierarchyRow> rowsForBothProjects() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry(CLOSED_PROJECT, "storyOfTheClosedProject",
                Collections.singletonList(SESSION_UNDER_THE_CLOSED_PROJECT)),
            new SessionDefinitionEntry(OPEN_PROJECT, "storyOfTheOpenProject",
                Collections.singletonList(SESSION_UNDER_THE_OPEN_PROJECT)));

        return builder.build(
            Arrays.asList(SESSION_UNDER_THE_CLOSED_PROJECT, SESSION_UNDER_THE_OPEN_PROJECT),
            entries, NA);
    }

    @Test
    public void onlyTheSessionsUnderAClosedProjectAreNamed() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Collections.singletonList(CLOSED_PROJECT));

        Set<String> named = SessionHierarchyBuilder.collapsedProjectSessionNames(
            rowsForBothProjects(), collapsedProjectKeys);

        Assert.assertTrue("the session under the project the owner closed is out of sight and its shell"
                + " process may be released", named.contains(SESSION_UNDER_THE_CLOSED_PROJECT));
        Assert.assertFalse("the session under a project the owner left open is on screen and its shell"
                + " process must stay", named.contains(SESSION_UNDER_THE_OPEN_PROJECT));
    }

    @Test
    public void noSessionIsNamedWhileEveryProjectIsOpen() {
        Set<String> named = SessionHierarchyBuilder.collapsedProjectSessionNames(
            rowsForBothProjects(), Collections.<String>emptySet());

        Assert.assertEquals("naming a session here would release the shell process of a session the"
                + " owner can see", 0, named.size());
    }

    @Test
    public void noSessionIsNamedWhileTheRowsAreNotBuiltYet() {
        Set<String> collapsedProjectKeys = new LinkedHashSet<>(Collections.singletonList(CLOSED_PROJECT));

        Set<String> named = SessionHierarchyBuilder.collapsedProjectSessionNames(
            Collections.<SessionHierarchyRow>emptyList(), collapsedProjectKeys);

        Assert.assertEquals("the session list is empty until it has been built, and naming every"
                + " session in that state would release the shell process of every session the app"
                + " holds", 0, named.size());
    }
}
