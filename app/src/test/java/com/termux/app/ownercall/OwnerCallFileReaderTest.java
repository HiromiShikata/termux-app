package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallFileReaderTest {

    private static final String FILE_URL = "https://calls.example.test/call-to-user/test/file.yaml?k=token";

    private static final String DOCUMENT = "---\n"
        + "sessionName: \"voicetest1\"\n"
        + "calledAt: \"2026-11-11T12:50:00Z\"\n"
        + "body: |2\n"
        + "  first call body\n"
        + "---\n"
        + "sessionName: \"voicetest2\"\n"
        + "calledAt: \"2026-11-11T12:51:00Z\"\n"
        + "body: |2\n"
        + "  second call body\n";

    private static final class FixedTransport implements OwnerCallFileTransport {
        final List<String> fetchedUrls = new ArrayList<>();
        final String document;
        final IOException failure;

        FixedTransport(String document, IOException failure) {
            this.document = document;
            this.failure = failure;
        }

        @Override
        public String fetch(String url) throws IOException {
            fetchedUrls.add(url);
            if (failure != null) {
                throw failure;
            }
            return document;
        }

        @Override
        public void delete(String url) {
        }
    }

    @Test
    public void readsOnlyTheCallsNamingTheRequestedSessionFromItsFile() throws IOException {
        FixedTransport transport = new FixedTransport(DOCUMENT, null);

        List<OwnerCall> calls = new OwnerCallFileReader(transport).readCallsOfSession(FILE_URL, "voicetest2");

        assertEquals(List.of(FILE_URL), transport.fetchedUrls);
        assertEquals(1, calls.size());
        assertEquals("second call body", calls.get(0).getBody());
    }

    @Test
    public void rethrowsTheTransportFailureSoTheCallerCanReportIt() {
        FixedTransport transport = new FixedTransport("", new IOException("unreachable"));

        IOException thrown = assertThrows(IOException.class,
            () -> new OwnerCallFileReader(transport).readCallsOfSession(FILE_URL, "voicetest1"));

        assertEquals("unreachable", thrown.getMessage());
    }
}
