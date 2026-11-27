package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserAlertTextTest {

    @Test
    public void firstPostCarriesTheBodyWhenItCanBeReadAndTheCallTextOtherwise() {
        String[][] cases = {
            {"Please review the plan.", "🔴", "Please review the plan."},
            {"Only the body", "Call text", "Only the body"},
            {null, "🔴", "🔴"},
            {null, "Call text", "Call text"},
        };
        for (String[] testCase : cases) {
            Assert.assertEquals(testCase[2], CallToUserAlertText.forFirstPost(testCase[0], testCase[1]));
        }
    }
}
