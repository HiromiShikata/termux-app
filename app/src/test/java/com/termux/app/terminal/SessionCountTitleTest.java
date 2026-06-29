package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionCountTitleTest {

    @Test
    public void sessionCountTitleAppendsPendingCallOverVisibleFractionInParentheses() {
        Assert.assertEquals("Sessions (1/3)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 1, 3));
    }

    @Test
    public void sessionCountTitleShowsZeroOverZeroWhenNoVisibleSessions() {
        Assert.assertEquals("Sessions (0/0)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0, 0));
    }

    @Test
    public void sessionCountTitleShowsZeroPendingCallsWhenNoVisibleSessionIsCalling() {
        Assert.assertEquals("Sessions (0/5)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0, 5));
    }

    @Test
    public void sessionCountTitleDenominatorIsTheVisibleCountWhenHiddenSessionsAreFilteredOut() {
        Assert.assertEquals("Sessions (3/22)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 3, 22));
    }

    @Test
    public void sessionCountTitleHasNoSecondShownOverTotalNumber() {
        String title = SessionListBottomSheetController.sessionCountTitle("Sessions", 2, 22);

        Assert.assertEquals("Sessions (2/22)", title);
        Assert.assertEquals("the header must contain exactly one fraction and no appended shown/total number",
            1, title.split("/").length - 1);
    }

    @Test
    public void filterToggleIconIsActiveFunnelWhenHidingHiddenSessions() {
        Assert.assertEquals(com.termux.R.drawable.ic_filter_alt,
            SessionListBottomSheetController.hiddenToggleIconResource(true));
    }

    @Test
    public void filterToggleIconIsFilterOffFunnelWhenShowingAllSessions() {
        Assert.assertEquals(com.termux.R.drawable.ic_filter_alt_off,
            SessionListBottomSheetController.hiddenToggleIconResource(false));
    }

    @Test
    public void filterToggleIconsForTheTwoStatesAreDifferentDrawables() {
        Assert.assertNotEquals(SessionListBottomSheetController.hiddenToggleIconResource(true),
            SessionListBottomSheetController.hiddenToggleIconResource(false));
    }

    @Test
    public void filterToggleContentDescriptionIsFilterOnWhenHidingHiddenSessions() {
        Assert.assertEquals(com.termux.R.string.action_session_filter_on,
            SessionListBottomSheetController.hiddenToggleContentDescriptionResource(true));
    }

    @Test
    public void filterToggleContentDescriptionIsFilterOffWhenShowingAllSessions() {
        Assert.assertEquals(com.termux.R.string.action_session_filter_off,
            SessionListBottomSheetController.hiddenToggleContentDescriptionResource(false));
    }

}
