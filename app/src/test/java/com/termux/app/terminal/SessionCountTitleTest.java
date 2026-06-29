package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionCountTitleTest {

    @Test
    public void sessionCountTitleAppendsCallOverTotalFractionInParentheses() {
        Assert.assertEquals("Sessions (1/3)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 1, 3, 3, false));
    }

    @Test
    public void sessionCountTitleShowsZeroOverZeroWhenNoSessions() {
        Assert.assertEquals("Sessions (0/0)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0, 0, 0, false));
    }

    @Test
    public void sessionCountTitleShowsZeroCallsWhenNoSessionIsCalling() {
        Assert.assertEquals("Sessions (0/5)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0, 5, 5, false));
    }

    @Test
    public void sessionCountTitleAppendsShownOverTotalWhenHidingHiddenSessions() {
        Assert.assertEquals("Sessions (2/25) 22/25",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 2, 22, 25, true));
    }

    @Test
    public void sessionCountTitleOmitsShownFractionWhenNotHidingHiddenSessions() {
        Assert.assertEquals("Sessions (2/25)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 2, 25, 25, false));
    }

    @Test
    public void hiddenToggleIconIsVisibilityOffWhenHidingHiddenSessions() {
        Assert.assertEquals(com.termux.R.drawable.ic_visibility_off,
            SessionListBottomSheetController.hiddenToggleIconResource(true));
    }

    @Test
    public void hiddenToggleIconIsVisibilityWhenShowingHiddenSessions() {
        Assert.assertEquals(com.termux.R.drawable.ic_visibility,
            SessionListBottomSheetController.hiddenToggleIconResource(false));
    }

}
