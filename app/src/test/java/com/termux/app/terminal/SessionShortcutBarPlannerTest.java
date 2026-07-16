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

public class SessionShortcutBarPlannerTest {

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

    private static List<String> targetSessionNames(List<SessionShortcut> shortcuts) {
        List<String> result = new ArrayList<>();
        for (SessionShortcut shortcut : shortcuts) {
            result.add(shortcut.getTargetSessionName());
        }
        return result;
    }

    @Test
    public void rightToLeftOrderPlacesAlwaysNaGroupBeforeProjectPmGroup() {
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("umino"), entry("xmile"));
        Set<String> alwaysNaSessionNames = namesInOrder("na1", "na2");

        List<SessionShortcut> shortcuts = planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        Assert.assertEquals(Arrays.asList("na1", "na2", "umino", "xmile"), labels(shortcuts));
    }

    @Test
    public void projectPmButtonTargetSessionNameDerivesFromExistingPlanner() {
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("umino"), entry("xcare"));
        Set<String> alwaysNaSessionNames = Collections.emptySet();

        List<SessionShortcut> shortcuts = planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        Assert.assertEquals(Arrays.asList("umino", "xcare"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList("uminopm", "xcarepm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaButtonLabelAndTargetSessionNameComeFromTheConfiguredList() {
        List<SessionDefinitionEntry> entries = Collections.emptyList();
        Set<String> alwaysNaSessionNames = namesInOrder("inbox", "review");

        List<SessionShortcut> shortcuts = planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        Assert.assertEquals(Arrays.asList("inbox", "review"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList("inbox", "review"), targetSessionNames(shortcuts));
    }

    @Test
    public void distinctProjectLabelsProduceOnePmShortcutEachInFirstSeenOrder() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            entry("umino"), entry("umino"), entry("xmile"));
        Set<String> alwaysNaSessionNames = Collections.emptySet();

        List<SessionShortcut> shortcuts = planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        Assert.assertEquals(Arrays.asList("umino", "xmile"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList("uminopm", "xmilepm"), targetSessionNames(shortcuts));
    }

    @Test
    public void blankProjectLabelsAndBlankAlwaysNaNamesAreSkipped() {
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("   "), entry("umino"));
        Set<String> alwaysNaSessionNames = namesInOrder("  ", "na");

        List<SessionShortcut> shortcuts = planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        Assert.assertEquals(Arrays.asList("na", "umino"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList("na", "uminopm"), targetSessionNames(shortcuts));
    }

    @Test
    public void noAlwaysNaAndNoProjectsProducesNoShortcuts() {
        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(Collections.emptySet(), Collections.emptyList());

        Assert.assertTrue(shortcuts.isEmpty());
    }

    @Test
    public void renderOrderReversesRightToLeftListSoFarRightGroupIsAddedLast() {
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("umino"), entry("xmile"));
        Set<String> alwaysNaSessionNames = namesInOrder("na1", "na2");
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);
        Set<String> presentSessionNames = namesInOrder("na1", "na2", "uminopm", "xmilepm");

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

        Assert.assertEquals(Arrays.asList("xmilepm", "uminopm", "na2", "na1"),
            targetSessionNames(renderOrderShortcuts));
    }

    @Test
    public void notPresentTargetSessionIsSkippedFromRenderedShortcuts() {
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("umino"), entry("xmile"));
        Set<String> alwaysNaSessionNames = namesInOrder("na1", "na2");
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);
        Set<String> presentSessionNames = namesInOrder("na1", "xmilepm");

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

        Assert.assertEquals(Arrays.asList("xmilepm", "na1"), targetSessionNames(renderOrderShortcuts));
        Assert.assertEquals(Arrays.asList("xmile", "na1"), labels(renderOrderShortcuts));
    }
}
