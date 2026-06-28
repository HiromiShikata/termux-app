package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionCountTitleTest {

    @Test
    public void sessionCountTitleAppendsCallOverTotalFractionInParentheses() {
        Assert.assertEquals("Sessions (1/3)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 1, 3));
    }

    @Test
    public void sessionCountTitleShowsZeroOverZeroWhenNoSessions() {
        Assert.assertEquals("Sessions (0/0)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0, 0));
    }

    @Test
    public void sessionCountTitleShowsZeroCallsWhenNoSessionIsCalling() {
        Assert.assertEquals("Sessions (0/5)",
            SessionListBottomSheetController.sessionCountTitle("Sessions", 0, 5));
    }

}
