package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessionShortcutBarRowsTest {

    private final SessionShortcutBarPlanner planner =
        new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());

    private static SessionDefinitionEntry entry(String groupLabel) {
        return new SessionDefinitionEntry(groupLabel, "story",
            Collections.singletonList("https://example.test/x"));
    }

    private static Set<String> namesInOrder(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    private static List<String> labels(List<SessionShortcut> shortcuts) {
        List<String> result = new ArrayList<>();
        for (SessionShortcut shortcut : shortcuts) {
            result.add(shortcut.getLabel());
        }
        return result;
    }

    @Test
    public void theTwoKindsOfShortcutArePlannedAsSeparateRows() {
        SessionShortcutRows rows = planner.planRightToLeftShortcutRows(
            namesInOrder("firstAlwaysSession", "secondAlwaysSession"),
            Arrays.asList(entry("firstProject"), entry("secondProject")),
            Collections.<String>emptyList());

        assertEquals("an always-on session shortcut that flows into the project manager row cannot be"
                + " told apart from a project manager shortcut",
            Arrays.asList("firstAlwaysSession", "secondAlwaysSession"),
            labels(rows.getAlwaysSessionShortcuts()));
        assertEquals(Arrays.asList("firstProject", "secondProject"),
            labels(rows.getProjectManagerSessionShortcuts()));
    }

    @Test
    public void eachRowKeepsTheRightToLeftOrderThePlannerAlreadyProduces() {
        SessionShortcutRows rows = planner.planRightToLeftShortcutRows(
            namesInOrder("firstAlwaysSession", "secondAlwaysSession"),
            Arrays.asList(entry("firstProject"), entry("secondProject")),
            Collections.<String>emptyList());

        assertEquals("the row is rendered by reversing this order, so a row planned in render order"
                + " would come out mirrored",
            Arrays.asList("secondAlwaysSession", "firstAlwaysSession"),
            labels(SessionShortcutBarPlanner.renderOrderShortcuts(rows.getAlwaysSessionShortcuts())));
        assertEquals(Arrays.asList("secondProject", "firstProject"),
            labels(SessionShortcutBarPlanner.renderOrderShortcuts(
                rows.getProjectManagerSessionShortcuts())));
    }

    @Test
    public void theProjectManagerRowCarriesTheSessionNameTheProjectResolvesTo() {
        SessionShortcutRows rows = planner.planRightToLeftShortcutRows(
            Collections.<String>emptySet(), Collections.singletonList(entry("firstProject")),
            Collections.<String>emptyList());

        assertEquals(1, rows.getProjectManagerSessionShortcuts().size());
        assertEquals("firstProjectpm",
            rows.getProjectManagerSessionShortcuts().get(0).getTargetSessionName());
    }

    @Test
    public void aConfigurationWithNoAlwaysOnSessionLeavesThatRowEmptyRatherThanBorrowingFromTheOther() {
        SessionShortcutRows rows = planner.planRightToLeftShortcutRows(
            Collections.<String>emptySet(), Collections.singletonList(entry("firstProject")),
            Collections.<String>emptyList());

        assertTrue("moving a project manager shortcut up into an empty always-on row would put it under"
            + " the wrong heading", rows.getAlwaysSessionShortcuts().isEmpty());
    }

    @Test
    public void theTwoRowsTogetherHoldExactlyWhatTheSingleRowHeldBefore() {
        Set<String> alwaysNaSessionNames = namesInOrder("firstAlwaysSession", "secondAlwaysSession");
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("firstProject"), entry("secondProject"));

        SessionShortcutRows rows = planner.planRightToLeftShortcutRows(alwaysNaSessionNames, entries,
            Collections.<String>emptyList());
        List<SessionShortcut> combined = new ArrayList<>(rows.getAlwaysSessionShortcuts());
        combined.addAll(rows.getProjectManagerSessionShortcuts());

        assertEquals("splitting the bar into rows must not add, drop or reorder a shortcut",
            labels(planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries,
                Collections.<String>emptyList())),
            labels(combined));
    }
}
