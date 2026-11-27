package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallAlertBodyTest {

    private static final String FILE_URL = "https://example.invalid/owner-calls/worker";
    private static final long NOW_MILLIS = 1795744800000L;
    private static final long ANSWERED_THROUGH_MILLIS = 1795741200000L;

    private static final class FixedTransport implements OwnerCallFileTransport {
        private final String mDocument;

        FixedTransport(String document) {
            mDocument = document;
        }

        @Override
        public String fetch(String url) {
            return mDocument;
        }

        @Override
        public void delete(String url) {
        }
    }

    private static final class FailingTransport implements OwnerCallFileTransport {
        @Override
        public String fetch(String url) throws IOException {
            throw new IOException("unreachable owner call file");
        }

        @Override
        public void delete(String url) {
        }
    }

    private static String call(String sessionName, String calledAt, String... bodyLines) {
        StringBuilder document = new StringBuilder("---\nsessionName: " + sessionName + "\ncalledAt: "
            + calledAt + "\nbody:\n");
        for (String line : bodyLines) {
            document.append("  ").append(line).append('\n');
        }
        return document.toString();
    }

    @Test
    public void resolveReturnsTheDisplayedBodyOfTheNewestUnansweredCallWithoutTheMarkerLine() throws IOException {
        String[][] cases = {
            {
                call("worker", "2026-11-27T00:00:00Z", "🔴", "Old question") + call("worker",
                    "2026-11-27T02:00:00Z", "🔴", "Please review the plan.", "Reply with yes or no."),
                "Please review the plan.\nReply with yes or no.",
            },
            {
                call("worker", "2026-11-27T02:00:00Z", "🔴", "Only call"),
                "Only call",
            },
            {
                call("worker", "2026-11-27T00:30:00Z", "🔴", "Answered already")
                    + call("worker", "2026-11-27T02:00:00Z", "🔴", "Still waiting"),
                "Still waiting",
            },
        };
        for (String[] testCase : cases) {
            String body = OwnerCallAlertBody.resolve(new FixedTransport(testCase[0]), FILE_URL, "worker",
                ANSWERED_THROUGH_MILLIS, NOW_MILLIS);
            Assert.assertEquals(testCase[1], body);
        }
    }

    @Test
    public void resolveReturnsNullWhenNoCallOfTheSessionIsStillUnanswered() throws IOException {
        String[][] cases = {
            {call("worker", "2026-11-27T00:30:00Z", "🔴", "Answered already"), "worker"},
            {call("other", "2026-11-27T02:00:00Z", "🔴", "Another session"), "worker"},
            {"", "worker"},
        };
        for (String[] testCase : cases) {
            String body = OwnerCallAlertBody.resolve(new FixedTransport(testCase[0]), FILE_URL, testCase[1],
                ANSWERED_THROUGH_MILLIS, NOW_MILLIS);
            Assert.assertNull(body);
        }
    }

    @Test
    public void resolvePropagatesFetchFailureSoTheCallerCanLogIt() {
        try {
            OwnerCallAlertBody.resolve(new FailingTransport(), FILE_URL, "worker", null, NOW_MILLIS);
            Assert.fail("a fetch failure must not be swallowed");
        } catch (IOException expected) {
            Assert.assertEquals("unreachable owner call file", expected.getMessage());
        }
    }
}
