package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultProjectManagerSessionPlannerTest {

    private final DefaultProjectManagerSessionPlanner planner = new DefaultProjectManagerSessionPlanner();

    @Test
    public void sessionNameForProjectLabelAppendsPmSuffix() {
        Assert.assertEquals("uminopm", planner.sessionNameForProjectLabel("umino"));
        Assert.assertEquals("xmilepm", planner.sessionNameForProjectLabel("xmile"));
        Assert.assertEquals("xcarepm", planner.sessionNameForProjectLabel("xcare"));
        Assert.assertEquals("utagepm", planner.sessionNameForProjectLabel("utage"));
    }

    @Test
    public void sessionNameForProjectLabelTrimsSurroundingWhitespace() {
        Assert.assertEquals("uminopm", planner.sessionNameForProjectLabel("  umino  "));
    }

    @Test
    public void sessionNameForProjectLabelReturnsNullForNullOrBlankLabel() {
        Assert.assertNull(planner.sessionNameForProjectLabel(null));
        Assert.assertNull(planner.sessionNameForProjectLabel(""));
        Assert.assertNull(planner.sessionNameForProjectLabel("   "));
    }

    @Test
    public void planSessionNamesProducesOnePmSessionPerUniqueProjectInFirstSeenOrder() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("umino", "storyA",
            Collections.singletonList("https://example.test/u1")));
        entries.add(new SessionDefinitionEntry("xmile", "storyB",
            Collections.singletonList("https://example.test/x1")));

        List<String> sessionNames = planner.planSessionNames(entries);

        Assert.assertEquals(Arrays.asList("uminopm", "xmilepm"), sessionNames);
    }

    @Test
    public void planSessionNamesDeduplicatesProjectsThatSpanMultipleStories() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("umino", "storyA",
            Collections.singletonList("https://example.test/u1")));
        entries.add(new SessionDefinitionEntry("umino", "storyB",
            Collections.singletonList("https://example.test/u2")));
        entries.add(new SessionDefinitionEntry("xmile", "storyC",
            Collections.singletonList("https://example.test/x1")));

        List<String> sessionNames = planner.planSessionNames(entries);

        Assert.assertEquals(Arrays.asList("uminopm", "xmilepm"), sessionNames);
    }

    @Test
    public void planSessionNamesSkipsEntriesWithoutProjectLabel() {
        List<SessionDefinitionEntry> entries = new ArrayList<>();
        entries.add(new SessionDefinitionEntry("", "storyA",
            Collections.singletonList("https://example.test/a1")));

        List<String> sessionNames = planner.planSessionNames(entries);

        Assert.assertTrue(sessionNames.isEmpty());
    }
}
