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
}
