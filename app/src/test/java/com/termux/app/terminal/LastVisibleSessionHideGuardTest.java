package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class LastVisibleSessionHideGuardTest {

    private static final String SESSION_BEING_HIDDEN = "session-current";

    private static final String ANOTHER_SESSION = "session-other";

    @Test
    public void theOnlyLiveSessionLeavesNothingVisibleBehindIt() {
        assertFalse("hiding the only live session would leave the application with nothing to show, so "
                + "the guard must report that no visible session remains",
            LastVisibleSessionHideGuard.hidingLeavesAVisibleSession(SESSION_BEING_HIDDEN,
                Collections.singletonList(SESSION_BEING_HIDDEN), Collections.emptySet()));
    }

    @Test
    public void anotherLiveAndShownSessionRemainsVisible() {
        assertTrue("another live session that is not itself hidden is somewhere for the view to move to, "
                + "so the hide must be allowed to take effect",
            LastVisibleSessionHideGuard.hidingLeavesAVisibleSession(SESSION_BEING_HIDDEN,
                Arrays.asList(SESSION_BEING_HIDDEN, ANOTHER_SESSION), Collections.emptySet()));
    }

    @Test
    public void anotherLiveSessionThatIsItselfHiddenIsNotSomewhereToMoveTo() {
        assertFalse("a live session that is already recorded as hidden is not a session the owner may be "
                + "shown, so it does not make the hide safe",
            LastVisibleSessionHideGuard.hidingLeavesAVisibleSession(SESSION_BEING_HIDDEN,
                Arrays.asList(SESSION_BEING_HIDDEN, ANOTHER_SESSION), hiddenNames(ANOTHER_SESSION)));
    }

    @Test
    public void aSessionNameThatIsAbsentFromTheLiveListDoesNotConsumeTheLastVisibleSession() {
        assertTrue("hiding a name that holds no live session must not be judged against itself; the live "
                + "session that remains is still somewhere for the view to move to",
            LastVisibleSessionHideGuard.hidingLeavesAVisibleSession("session-without-a-live-object",
                Collections.singletonList(ANOTHER_SESSION), Collections.emptySet()));
    }

    @Test
    public void liveEntriesWithoutAResolvedNameAreNotCountedAsVisibleSessions() {
        assertFalse("a live entry whose session carries no name cannot be shown to the owner, so it must "
                + "not be counted as the session the view would move to",
            LastVisibleSessionHideGuard.hidingLeavesAVisibleSession(SESSION_BEING_HIDDEN,
                Arrays.asList(SESSION_BEING_HIDDEN, null, ""), Collections.emptySet()));
    }

    private static Set<String> hiddenNames(String... sessionNames) {
        return new LinkedHashSet<>(Arrays.asList(sessionNames));
    }
}
