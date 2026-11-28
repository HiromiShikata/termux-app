package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class HttpSessionDefinitionDocumentFetcherTest {

    private final HttpSessionDefinitionDocumentFetcher fetcher = new HttpSessionDefinitionDocumentFetcher();

    @Test
    public void fetchThrowsIOExceptionForNonHttpProtocol() {
        try {
            fetcher.fetch("file:///etc/hostname");
            Assert.fail("Expected IOException for non-HTTP protocol");
        } catch (IOException expected) {
            Assert.assertTrue(expected.getMessage().contains("Unsupported protocol"));
        }
    }

    @Test
    public void redactAccessTokenReplacesOnlyTheKeyValue() {
        String[][] inputAndExpected = {
            {"https://example.test:9443/in-tmux-by-human/alpha.v3.json?k=SECRET", "https://example.test:9443/in-tmux-by-human/alpha.v3.json?k=<redacted>"},
            {"https://example.test/a.json?x=1&k=SECRET&y=2", "https://example.test/a.json?x=1&k=<redacted>&y=2"},
            {"https://example.test/a.json", "https://example.test/a.json"},
        };
        for (String[] testCase : inputAndExpected) {
            Assert.assertEquals("input " + testCase[0], testCase[1],
                HttpSessionDefinitionDocumentFetcher.redactAccessToken(testCase[0]));
        }
    }

    @Test
    public void errorMessagesNeverCarryTheAccessTokenValue() {
        String[][] inputAndExpected = {
            {"file:///etc/hostname?k=SECRET", "Unsupported protocol for session definition URL: file:///etc/hostname?k=<redacted>"},
        };
        for (String[] testCase : inputAndExpected) {
            Assert.assertEquals("input " + testCase[0], testCase[1],
                HttpSessionDefinitionDocumentFetcher.unsupportedProtocolMessage(testCase[0]));
        }
        Assert.assertEquals("Unexpected HTTP response code 401 for https://example.test:9443/in-tmux-by-human/alpha.v3.json?k=<redacted>",
            HttpSessionDefinitionDocumentFetcher.unexpectedResponseMessage(401,
                "https://example.test:9443/in-tmux-by-human/alpha.v3.json?k=SECRET"));
    }

    @Test
    public void fetchOfUnsupportedProtocolReportsTheRedactedUrl() {
        try {
            fetcher.fetch("file:///etc/hostname?k=SECRET");
            Assert.fail("Expected IOException for non-HTTP protocol");
        } catch (IOException expected) {
            Assert.assertFalse(expected.getMessage(), expected.getMessage().contains("SECRET"));
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("k=<redacted>"));
        }
    }
}
