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
        Set<String> presentSessionNames = namesInOrder("uminopm", url);

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

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
    public void alwaysNaCompositeWithNoLiveSessionKeepsTheConfiguredNameSoThePresentFilterDropsIt() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("umino/story");
        List<String> liveSessionNames = Collections.singletonList("uminopm");
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);
        Set<String> presentSessionNames = namesInOrder("uminopm");

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

        Assert.assertEquals(Collections.singletonList("umino"), labels(renderOrderShortcuts));
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

    @Test
    public void alwaysNaConfiguredByBareProjectLabelMatchesTheLiveUrlSessionOfThatProject() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("secretary", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("secretary");
        List<String> liveSessionNames = Arrays.asList("secretarypm", url);

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Arrays.asList("secretary", "secretary"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList(url, "secretarypm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaMatchedByBareProjectLabelRendersInRightMostGroupAheadOfPm() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("secretary", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("secretary");
        List<String> liveSessionNames = Arrays.asList("secretarypm", url);
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);
        Set<String> presentSessionNames = namesInOrder("secretarypm", url);

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

        Assert.assertEquals(Arrays.asList("secretary", "secretary"), labels(renderOrderShortcuts));
        Assert.assertEquals(Arrays.asList("secretarypm", url), targetSessionNames(renderOrderShortcuts));
    }

    @Test
    public void alwaysNaConfiguredByResolvedTitleMatchesTheLiveUrlSession() {
        String url = "https://github.com/HiromiShikata/secretary";
        java.util.Map<String, String> titlesByUrl = new java.util.HashMap<>();
        titlesByUrl.put(url, "inbox-title");
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(url), titlesByUrl));
        Set<String> alwaysNaSessionNames = namesInOrder("inbox-title");
        List<String> liveSessionNames = Arrays.asList("uminopm", url);

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Arrays.asList("inbox-title", "umino"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList(url, "uminopm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaConfiguredNameMatchingNoLiveSessionRendersNoButtonAfterPresentFilter() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("secretary", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("nonexistent");
        List<String> liveSessionNames = Arrays.asList("secretarypm", url);
        List<SessionShortcut> rightToLeftShortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);
        Set<String> presentSessionNames = namesInOrder("secretarypm", url);

        List<SessionShortcut> renderOrderShortcuts =
            SessionShortcutBarPlanner.renderOrderPresentShortcuts(rightToLeftShortcuts, presentSessionNames);

        Assert.assertEquals(Collections.singletonList("secretary"), labels(renderOrderShortcuts));
        Assert.assertEquals(Collections.singletonList("secretarypm"),
            targetSessionNames(renderOrderShortcuts));
    }

    @Test
    public void aLiveSessionMatchingMultipleCriteriaRendersOnlyOneAlwaysNaButton() {
        String url = "https://github.com/HiromiShikata/secretary";
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("secretary", "story", url));
        Set<String> alwaysNaSessionNames = namesInOrder("secretary", "secretary/story");
        List<String> liveSessionNames = Arrays.asList("secretarypm", url);

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Arrays.asList("secretary", "secretary"), labels(shortcuts));
        Assert.assertEquals(Arrays.asList(url, "secretarypm"), targetSessionNames(shortcuts));
    }

    @Test
    public void alwaysNaMatchingASessionThatIsAlreadyAPmTargetDoesNotDoubleRender() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(entry("umino"));
        Set<String> alwaysNaSessionNames = namesInOrder("uminopm");
        List<String> liveSessionNames = Collections.singletonList("uminopm");

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals(Collections.singletonList("umino"), labels(shortcuts));
        Assert.assertEquals(Collections.singletonList("uminopm"), targetSessionNames(shortcuts));
    }
}
