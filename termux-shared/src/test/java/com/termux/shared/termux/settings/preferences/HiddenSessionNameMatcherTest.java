package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class HiddenSessionNameMatcherTest {

    private static final Set<String> HIDDEN_SESSION_NAMES =
        new LinkedHashSet<>(Collections.singletonList("hidden-session"));

    @Test
    public void aRecordedHiddenNameMatches() {
        assertTrue(HiddenSessionNameMatcher.matchesAHiddenSession(
            "hidden-session", HIDDEN_SESSION_NAMES));
    }

    @Test
    public void aNameThatIsNotRecordedHiddenDoesNotMatch() {
        assertFalse(HiddenSessionNameMatcher.matchesAHiddenSession(
            "ordinary-session", HIDDEN_SESSION_NAMES));
    }

    @Test
    public void anAbsentNameDoesNotMatch() {
        assertFalse(HiddenSessionNameMatcher.matchesAHiddenSession(null, HIDDEN_SESSION_NAMES));
    }

    @Test
    public void noNameMatchesWhenNothingIsHidden() {
        assertFalse(HiddenSessionNameMatcher.matchesAHiddenSession(
            "hidden-session", Collections.emptySet()));
    }
}
