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

public class SessionShortcutBarHidingOneSessionKeepsEveryOtherShortcutTest {

    private static final String COMPOSITE_SESSION_NAME = "umino/story";
    private static final String URL_SESSION_NAME = "https://github.com/HiromiShikata/secretary";
    private static final String OTHER_URL_SESSION_NAME = "https://github.com/HiromiShikata/termux-app";
    private static final String PROJECT_MANAGER_SESSION_NAME = "uminopm";

    private final SessionShortcutBarPlanner planner =
        new SessionShortcutBarPlanner(new DefaultProjectManagerSessionPlanner());

    @Test
    public void theLiveUrlSessionKeepsItsOwnShortcutWhileTheCompositeSessionNamingItIsHidden() {
        List<SessionDefinitionEntry> entries = Collections.singletonList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(URL_SESSION_NAME)));
        Set<String> alwaysNaSessionNames = namesInOrder(COMPOSITE_SESSION_NAME, URL_SESSION_NAME);
        List<String> liveSessionNames =
            Arrays.asList(URL_SESSION_NAME, PROJECT_MANAGER_SESSION_NAME);

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals("hiding the composite session must not take away the shortcut of the url "
                + "session its entry names, which is still live and was never hidden",
            Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME, "umino"), labels(shortcuts));
        Assert.assertEquals("a shortcut must navigate to the session its own label names, so the hidden "
                + "composite name must not be rendered as the label of the still live url session",
            Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME, PROJECT_MANAGER_SESSION_NAME),
            targetSessionNames(shortcuts));
    }

    @Test
    public void hidingOneSessionLeavesEveryProjectManagerShortcutAndEveryOtherSessionShortcutDisplayed() {
        List<SessionDefinitionEntry> entries = Arrays.asList(
            new SessionDefinitionEntry("umino", "story", Collections.singletonList(URL_SESSION_NAME)),
            new SessionDefinitionEntry("xmile", "story",
                Collections.singletonList(OTHER_URL_SESSION_NAME)));
        Set<String> alwaysNaSessionNames =
            namesInOrder(COMPOSITE_SESSION_NAME, URL_SESSION_NAME, OTHER_URL_SESSION_NAME);
        List<String> liveSessionNames = Arrays.asList(URL_SESSION_NAME, OTHER_URL_SESSION_NAME,
            PROJECT_MANAGER_SESSION_NAME, "xmilepm");

        List<SessionShortcut> shortcuts =
            planner.planRightToLeftShortcuts(alwaysNaSessionNames, entries, liveSessionNames);

        Assert.assertEquals("hiding one session costs at most that session's own shortcut, so every "
                + "project manager shortcut and every other configured session's shortcut stays planned",
            Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME, OTHER_URL_SESSION_NAME,
                "umino", "xmile"),
            labels(shortcuts));
        Assert.assertEquals(Arrays.asList(COMPOSITE_SESSION_NAME, URL_SESSION_NAME,
                OTHER_URL_SESSION_NAME, PROJECT_MANAGER_SESSION_NAME, "xmilepm"),
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
