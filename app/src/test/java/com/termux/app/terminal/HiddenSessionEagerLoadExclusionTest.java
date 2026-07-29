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
        assertTrue(HiddenSessionEagerLoadExclusion.isExcludedFromEagerLoad(
            "hidden-session", HIDDEN_SESSION_NAMES));
    }

    @Test
    public void ordinarySessionIsStillEagerLoaded() {
        assertFalse(HiddenSessionEagerLoadExclusion.isExcludedFromEagerLoad(
            "ordinary-session", HIDDEN_SESSION_NAMES));
    }

    @Test
    public void unnamedSessionIsStillEagerLoaded() {
        assertFalse(HiddenSessionEagerLoadExclusion.isExcludedFromEagerLoad(null, HIDDEN_SESSION_NAMES));
    }

    @Test
    public void everySessionIsEagerLoadedWhenNothingIsHidden() {
        assertFalse(HiddenSessionEagerLoadExclusion.isExcludedFromEagerLoad(
            "hidden-session", Collections.emptySet()));
    }
}
