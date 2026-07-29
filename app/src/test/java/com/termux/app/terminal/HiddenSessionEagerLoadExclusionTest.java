package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class HiddenSessionEagerLoadExclusionTest {

    private static final Set<String> HIDDEN_SESSION_NAMES =
        new LinkedHashSet<>(Collections.singletonList("hidden-session"));

    @Test
    public void hiddenSessionIsExcludedFromTheStartupEagerLoadPass() {
        assertFalse(HiddenSessionEagerLoadExclusion.shouldEagerLoadSession(
            "hidden-session", HIDDEN_SESSION_NAMES));
    }

    @Test
    public void ordinarySessionIsStillEagerLoaded() {
        assertTrue(HiddenSessionEagerLoadExclusion.shouldEagerLoadSession(
            "ordinary-session", HIDDEN_SESSION_NAMES));
    }

    @Test
    public void unnamedSessionIsStillEagerLoaded() {
        assertTrue(HiddenSessionEagerLoadExclusion.shouldEagerLoadSession(null, HIDDEN_SESSION_NAMES));
    }

    @Test
    public void everySessionIsEagerLoadedWhenNothingIsHidden() {
        assertTrue(HiddenSessionEagerLoadExclusion.shouldEagerLoadSession(
            "hidden-session", Collections.emptySet()));
    }
}
