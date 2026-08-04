package com.termux.shared.termux.settings.preferences;

import org.junit.Assert;
import org.junit.Test;

public class UserRemovedSessionHideWindowTest {

    private static final long NOW_MILLIS = 1_700_000_000_000L;

    @Test
    public void theWindowTheOwnerAskedForLastsFifteenMinutes() {
        Assert.assertEquals(15 * 60 * 1000L, UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS);
    }

    @Test
    public void aSessionNobodyDeletedIsNotHidden() {
        Assert.assertFalse(UserRemovedSessionHideWindow.hidesSession(null, NOW_MILLIS));
    }

    @Test
    public void aSessionDeletedThisInstantIsHidden() {
        Assert.assertTrue(UserRemovedSessionHideWindow.hidesSession(NOW_MILLIS, NOW_MILLIS));
    }

    @Test
    public void aSessionDeletedOneMillisecondBeforeTheWindowEndsIsStillHidden() {
        Assert.assertTrue(UserRemovedSessionHideWindow.hidesSession(
            NOW_MILLIS - UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS + 1L, NOW_MILLIS));
    }

    @Test
    public void aSessionDeletedAFullFifteenMinutesAgoIsNoLongerHidden() {
        Assert.assertFalse(UserRemovedSessionHideWindow.hidesSession(
            NOW_MILLIS - UserRemovedSessionHideWindow.HIDE_DURATION_MILLIS, NOW_MILLIS));
    }
}
