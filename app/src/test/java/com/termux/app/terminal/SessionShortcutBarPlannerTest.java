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

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);

        Assert.assertEquals(Arrays.asList("xmilepm", "uminopm", "na2", "na1"),
            targetSessionNames(renderOrderShortcuts));
    }

    @Test
    public void aTargetSessionThatDoesNotExistYetStillRendersItsShortcut() {
        List<SessionDefinitionEntry> entries = Arrays.asList(entry("umino"), entry("xmile"));
        Set<String> alwaysNaSessionNames = namesInOrder("na1", "na2");
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);

        Assert.assertEquals("every shortcut the session definition and the pinned-name configuration"
                + " ask for must be rendered; whether its target session already exists decides only"
                + " what tapping it does",
            Arrays.asList("xmilepm", "uminopm", "na2", "na1"),
            targetSessionNames(renderOrderShortcuts));
        Assert.assertEquals(Arrays.asList("xmile", "umino", "na2", "na1"),
            labels(renderOrderShortcuts));
    }

    private static SessionDefinitionEntry entry(String groupLabel, String entryLabel, String... urls) {
        return new SessionDefinitionEntry(groupLabel, entryLabel, Arrays.asList(urls));
    }

    @Test
    public void alwaysNaConfiguredByCompositeSessionNameResolvesToTheLiveUrlSession() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("umino/story");
        List<String> liveSessionNames = Arrays.asList("uminopm", url);

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Arrays.asList("umino/story", "umino"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList(url, "uminopm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaCompositeSessionThatExistsRendersAButtonInRightMostGroupAheadOfPm() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("umino/story");
        List<String> liveSessionNames = Arrays.asList("uminopm", url);
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);

        Assert.assertEquals(Arrays.asList("umino", "umino/story"), labels(renderOrderShortcuts));
        Assert.assertEquals(Arrays.asList("uminopm", url), targetSessionNames(renderOrderShortcuts));
    }

    @Test
    public void alwaysNaConfiguredByExactLiveNameStillResolvesToThatName() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino"));
        Set<String> alwaysNaSessionNames = namesInOrder("na-inbox");
        List<String> liveSessionNames = Arrays.asList("uminopm", "na-inbox");

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Arrays.asList("na-inbox", "umino"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList("na-inbox", "uminopm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaCompositeWithNoLiveSessionKeepsTheConfiguredNameAndStillRendersIt() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("umino/story");
        List<String> liveSessionNames = Collections.singletonList("uminopm");
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderShortcuts(rightToLeftShortcuts);

        Assert.assertEquals("a pinned composite name with no live session keeps the configured name"
                + " and still renders its shortcut",
            Arrays.asList("umino", "umino/story"), labels(renderOrderShortcuts));
        Assert.assertEquals(Arrays.asList("uminopm", "umino/story"),
            targetSessionNames(renderOrderShortcuts));
    }

    @Test
    public void legacyLiveNameUnawarePlanningLeavesACompositeConfiguredNameUnresolvedSoItNeverMatchesAUrlSession() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("umino/story");

        List<SessionShortcut> shortcuts = planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries);

        Assert.assertEquals(Arrays.asList("umino/story", "uminopm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaCompositeEntryWithMultipleLiveUrlsIsAmbiguousSoTheConfiguredNameIsKept() {
        String firstUrl = "https://github.com/HiromiShikata/secretary/issues/1";
        String secondUrl = "https://github.com/HiromiShikata/secretary/issues/2";
        List<SessionDefinitionEntry> entries =
            Collections.singletonList(entry("umino", "story", firstUrl, secondUrl));
        Set<String> alwaysNaSessionNames = namesInOrder("umino/story");
        List<String> liveSessionNames = Arrays.asList("uminopm", firstUrl, secondUrl);

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Arrays.asList("umino/story", "uminopm"), targetSessionNames(shortcuts));
    }
}
