package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class BrowserSessionVisibilityStateTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";
    private static final String NAME_A = "name-a";
    private static final String NAME_B = "name-b";

    @Test
    public void aNewlySeenSessionDefaultsToTerminal() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
    }

    @Test
    public void rememberingBrowserVisibleIsScopedPerSession() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        Assert.assertTrue(state.wasBrowserVisible(SESSION_A));
        Assert.assertFalse(state.wasBrowserVisible(SESSION_B));
    }

    @Test
    public void closingTheBrowserClearsTheRememberedFlagForThatSession() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        state.setBrowserVisible(SESSION_A, false);
        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
    }

    @Test
    public void restoresBrowserWhenTargetHadBrowserVisibleAndHasActiveTab() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        Assert.assertTrue(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void showsTerminalWhenTargetHadBrowserVisibleButHasNoActiveTab() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, false));
    }

    @Test
    public void showsTerminalWhenTargetWasShowingTerminalEvenWithAnActiveTab() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void aPreparedUrlSessionRestoresItsBrowserOnFirstSwitchWithoutAnyManualShow() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));

        state.setBrowserVisible(SESSION_A, true);

        Assert.assertTrue(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void clearingASessionForgetsItsRememberedBrowserState() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        state.clearSession(SESSION_A);
        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void aNullSessionHandleIsNeverBrowserVisible() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(null, true);
        Assert.assertFalse(state.wasBrowserVisible(null));
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(null, true));
    }

    @Test
    public void rememberedStatesOfTwoSessionsAreIndependent() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        state.setBrowserVisible(SESSION_B, false);
        Assert.assertTrue(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_B, true));
    }

    @Test
    public void openingTheBrowserPersistsTheSessionNameToTheListener() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        AtomicReference<Set<String>> persisted = new AtomicReference<>();
        state.setPersistedNamesListener(persisted::set);

        state.setBrowserVisible(SESSION_A, NAME_A, true);

        Assert.assertNotNull(persisted.get());
        Assert.assertTrue(persisted.get().contains(NAME_A));
    }

    @Test
    public void closingTheBrowserPersistsTheRemovalToTheListener() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        AtomicReference<Set<String>> persisted = new AtomicReference<>();
        state.setPersistedNamesListener(persisted::set);

        state.setBrowserVisible(SESSION_A, NAME_A, true);
        state.setBrowserVisible(SESSION_A, NAME_A, false);

        Assert.assertNotNull(persisted.get());
        Assert.assertFalse(persisted.get().contains(NAME_A));
    }

    @Test
    public void aSessionSwitchDoesNotClearThePersistedOpenStateOfTheLeftSession() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, NAME_A, true);

        state.setBrowserVisible(SESSION_B, NAME_B, false);

        Assert.assertTrue(state.wasBrowserOpenForSessionName(NAME_A));
    }

    @Test
    public void seededPersistedNamesAreRememberedForCrossRestartRestore() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Set<String> seeded = new LinkedHashSet<>();
        seeded.add(NAME_A);
        state.setPersistedOpenSessionNames(seeded);

        Assert.assertTrue(state.wasBrowserOpenForSessionName(NAME_A));
        Assert.assertFalse(state.wasBrowserOpenForSessionName(NAME_B));
    }

    @Test
    public void seededPersistedNamesSurviveAFreshInMemoryHandleSet() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Set<String> seeded = new LinkedHashSet<>();
        seeded.add(NAME_A);
        state.setPersistedOpenSessionNames(seeded);

        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
        Assert.assertTrue(state.wasBrowserOpenForSessionName(NAME_A));
    }

    @Test
    public void clearingASessionAlsoForgetsItsPersistedOpenName() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        AtomicReference<Set<String>> persisted = new AtomicReference<>();
        state.setPersistedNamesListener(persisted::set);
        state.setBrowserVisible(SESSION_A, NAME_A, true);

        state.clearSession(SESSION_A, NAME_A);

        Assert.assertFalse(state.wasBrowserOpenForSessionName(NAME_A));
        Assert.assertNotNull(persisted.get());
        Assert.assertFalse(persisted.get().contains(NAME_A));
    }

    @Test
    public void aNullSessionNameIsNeverPersisted() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        AtomicReference<Set<String>> persisted = new AtomicReference<>();
        state.setPersistedNamesListener(persisted::set);

        state.setBrowserVisible(SESSION_A, null, true);

        Assert.assertNull(persisted.get());
        Assert.assertTrue(state.wasBrowserVisible(SESSION_A));
    }
}
