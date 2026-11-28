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
}
