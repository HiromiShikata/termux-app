package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogPagingTest {

    @Test
    public void showsWhichOfTheWaitingCallsIsOnScreen() {
        Assert.assertEquals("1 / 3", OwnerCallDialogPaging.resolve(0, 3).getPositionLabel());
        Assert.assertEquals("3 / 3", OwnerCallDialogPaging.resolve(2, 3).getPositionLabel());
    }

    @Test
    public void offersNoMoveBeyondTheOldestOrTheNewestCall() {
        OwnerCallDialogPaging oldest = OwnerCallDialogPaging.resolve(0, 3);
        OwnerCallDialogPaging newest = OwnerCallDialogPaging.resolve(2, 3);

        Assert.assertFalse(oldest.isPreviousEnabled());
        Assert.assertTrue(oldest.isNextEnabled());
        Assert.assertTrue(newest.isPreviousEnabled());
        Assert.assertFalse(newest.isNextEnabled());
    }

    @Test
    public void offersNoMoveAtAllWhileOneCallIsWaiting() {
        OwnerCallDialogPaging onlyCall = OwnerCallDialogPaging.resolve(0, 1);

        Assert.assertEquals("1 / 1", onlyCall.getPositionLabel());
        Assert.assertFalse(onlyCall.isPreviousEnabled());
        Assert.assertFalse(onlyCall.isNextEnabled());
    }

    @Test
    public void keepsARequestBeyondTheEndsOnTheNearestCall() {
        Assert.assertEquals(0, OwnerCallDialogPaging.resolve(-1, 3).getIndex());
        Assert.assertEquals(2, OwnerCallDialogPaging.resolve(9, 3).getIndex());
    }

    @Test
    public void reportsNoPositionWhileNoCallIsWaiting() {
        OwnerCallDialogPaging nothingWaiting = OwnerCallDialogPaging.resolve(0, 0);

        Assert.assertEquals(0, nothingWaiting.getIndex());
        Assert.assertEquals("0 / 0", nothingWaiting.getPositionLabel());
        Assert.assertFalse(nothingWaiting.isPreviousEnabled());
        Assert.assertFalse(nothingWaiting.isNextEnabled());
    }
}
