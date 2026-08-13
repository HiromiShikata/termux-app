package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OwnerCallDialogPagingTest {

    @Test
    public void showsTheOneBasedPositionAndTheTotal() {
        assertEquals("1 / 3", OwnerCallDialogPaging.resolve(0, 3).getPositionLabel());
        assertEquals("2 / 3", OwnerCallDialogPaging.resolve(1, 3).getPositionLabel());
        assertEquals("3 / 3", OwnerCallDialogPaging.resolve(2, 3).getPositionLabel());
    }

    @Test
    public void disablesThePreviousButtonOnTheFirstCall() {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(0, 3);

        assertFalse(paging.isPreviousEnabled());
        assertTrue(paging.isNextEnabled());
    }

    @Test
    public void disablesTheNextButtonOnTheLastCall() {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(2, 3);

        assertTrue(paging.isPreviousEnabled());
        assertFalse(paging.isNextEnabled());
    }

    @Test
    public void disablesBothButtonsWhenOnlyOneCallIsWaiting() {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(0, 1);

        assertEquals("1 / 1", paging.getPositionLabel());
        assertFalse(paging.isPreviousEnabled());
        assertFalse(paging.isNextEnabled());
    }

    @Test
    public void clampsAnIndexBeyondTheLastCallToTheLastCall() {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(9, 3);

        assertEquals(2, paging.getIndex());
        assertEquals("3 / 3", paging.getPositionLabel());
    }

    @Test
    public void clampsANegativeIndexToTheFirstCall() {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(-2, 3);

        assertEquals(0, paging.getIndex());
        assertEquals("1 / 3", paging.getPositionLabel());
    }

    @Test
    public void reportsNoPositionWhenNoCallIsWaiting() {
        OwnerCallDialogPaging paging = OwnerCallDialogPaging.resolve(0, 0);

        assertEquals("0 / 0", paging.getPositionLabel());
        assertFalse(paging.isPreviousEnabled());
        assertFalse(paging.isNextEnabled());
    }
}
