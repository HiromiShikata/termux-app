package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TermuxAppSharedPreferencesUserRemovedSessionTimesTest {

    private static final String DELETED_LONG_AGO = "https://example.test/deleted-long-ago";

    private static final String DELETED_JUST_NOW = "https://example.test/deleted-just-now";

    private TermuxAppSharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = TermuxAppSharedPreferences.build(RuntimeEnvironment.getApplication(), true);
        Assert.assertNotNull(preferences);
        preferences.setUserRemovedSessionTimes("");
    }

    @Test
    public void anInstallThatHasNeverHadADeletionCarriesNoRemovalRecords() {
        Assert.assertTrue("an existing install must behave exactly as it did before this record existed",
            preferences.getUserRemovedSessionTimes().isEmpty());
    }

    @Test
    public void aRecordedDeletionComesBackWithTheMomentItHappened() {
        preferences.recordSessionUserRemovedAt(DELETED_JUST_NOW, 1_700_000_000_000L);

        Assert.assertEquals(Long.valueOf(1_700_000_000_000L),
            preferences.getUserRemovedSessionTimes().get(DELETED_JUST_NOW));
    }

    @Test
    public void recordingADeletionDropsRecordsThatHaveLeftTheHideWindow() {
        long deletedLongAgo = 1_700_000_000_000L;
        preferences.recordSessionUserRemovedAt(DELETED_LONG_AGO, deletedLongAgo);

        preferences.recordSessionUserRemovedAt(DELETED_JUST_NOW,
            deletedLongAgo + UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS);

        Assert.assertFalse("a record that can no longer hide anything must not be kept forever, otherwise"
                + " the stored value grows with every session the owner ever deleted",
            preferences.getUserRemovedSessionTimes().containsKey(DELETED_LONG_AGO));
        Assert.assertTrue("a record that is still inside its window must survive the pruning",
            preferences.getUserRemovedSessionTimes().containsKey(DELETED_JUST_NOW));
    }

    @Test
    public void clearingARemovalTimeEndsTheHideForThatSessionOnly() {
        preferences.recordSessionUserRemovedAt(DELETED_LONG_AGO, 1_700_000_000_000L);
        preferences.recordSessionUserRemovedAt(DELETED_JUST_NOW, 1_700_000_000_000L);

        preferences.clearSessionUserRemovedTime(DELETED_JUST_NOW);

        Assert.assertFalse("the owner opening the session again ends the hide immediately",
            preferences.getUserRemovedSessionTimes().containsKey(DELETED_JUST_NOW));
        Assert.assertTrue("another session's hide must not be ended by it",
            preferences.getUserRemovedSessionTimes().containsKey(DELETED_LONG_AGO));
    }

    @Test
    public void recordingADeletionLeavesTheUserRemovedNameSetAlone() {
        preferences.recordSessionUserRemovedAt(DELETED_JUST_NOW, 1_700_000_000_000L);

        Assert.assertFalse("the removal time is a separate record; writing it must not change which"
                + " session names are suppressed from automatic reconnect",
            preferences.isSessionUserRemoved(DELETED_JUST_NOW));
    }
}
