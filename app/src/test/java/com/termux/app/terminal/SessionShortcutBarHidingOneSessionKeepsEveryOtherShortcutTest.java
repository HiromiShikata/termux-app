package com.termux.app.terminal;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SessionShortcutBarHidingOneSessionKeepsEveryOtherShortcutTest {

    private static final String COMPOSITE_SESSION_NAME = "umino/story";
    private static final String URL_SESSION_NAME = "https://github.com/HiromiShikata/secretary";
    private static final String OTHER_URL_SESSION_NAME = "https://github.com/HiromiShikata/termux-app";

    private final SessionShortcutBarPlanner planner = new SessionShortcutBarPlanner();

    @Test
    public void theLiveUrlSessionKeepsItsOwnShortcutWhileTheCompositeSessionNamingItIsHidden() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(URL_SESSION_NAME)));
        Set<String> alwaysNaSessionNames = namesInOrder(COMPOSITE_SESSION_NAME, URL_SESSION_NAME);
        List<String> liveSessionNames = Arrays.asList(URL_SESSION_NAME, "uminopm");

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals("hiding the composite session must not take away the shortcut of the url "
                + "session its entry names, which is still live and was never hidden",
            Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME), labels(shortcuts));
        Assert.assertEquals("a shortcut must navigate to the session its own label names",
            Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME),
            targetSessionNames(shortcuts));
    }

    @Test
    public void hidingOneSessionLeavesEveryOtherSessionShortcutDisplayed() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(URL_SESSION_NAME)),
            new SessionDefinitionEntry("xmile", "story",
                Collections.singletonList(OTHER_URL_SESSION_NAME)));
        Set<String> alwaysNaSessionNames =
            namesInOrder(COMPOSITE_SESSION_NAME, URL_SESSION_NAME, OTHER_URL_SESSION_NAME);
        List<String> liveSessionNames = Arrays.asList(URL_SESSION_NAME, OTHER_URL_SESSION_NAME,
            "uminopm", "xmilepm");

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals("hiding one session costs at most that session's own shortcut, so every "
                + "other configured session's shortcut stays planned",
            Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME, OTHER_URL_SESSION_NAME),
            labels(shortcuts));
        Assert.assertEquals(Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME,
                OTHER_URL_SESSION_NAME),
            targetSessionNames(shortcuts));
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
}
