package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class ConsoleAccessTokenHeaderTest {
    @Test
    public void requestHeadersForReturnsTheTokenHeaderOnlyForNonEmptyTokens() {
        String[][] tokenAndExpected = {{"test-token", "test-token"}, {null, null}, {"", null}};
        for (String[] testCase : tokenAndExpected) {
            Map<String, String> headers = ConsoleAccessTokenHeader.requestHeadersFor(testCase[0]);
            Map<String, String> expected = testCase[1] == null
                ? Collections.emptyMap()
                : Collections.singletonMap(ConsoleAccessTokenHeader.NAME, testCase[1]);
            assertEquals(expected, headers);
        }
    }
}
