package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ConsoleLiveToolCallForwarderTest {

    private static final String TOOL_CALLS_URL = "https://pc.example:8443/api/live/tool-calls?k=secret";

    static final class RecordingHttpClient implements LiveToolCallHttpClient {
        final List<String> urls = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        LiveSessionHttpResponse response = new LiveSessionHttpResponse(200, "{\"output\":\"ok\"}");
        IOException failure;

        @Override
        public LiveSessionHttpResponse post(String endpointUrl, String jsonBody) throws IOException {
            urls.add(endpointUrl);
            bodies.add(jsonBody);
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }

    private static ConsoleLiveToolCallForwarder forwarderFor(RecordingHttpClient http) {
        return new ConsoleLiveToolCallForwarder(http, TOOL_CALLS_URL);
    }

    @Test
    public void postsToToolCallsUrlWithNameArgumentsAndSessionName() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient();
        forwarderFor(http).forward("example_read_tool", "{\"limit\":3}", "alpha");
        assertEquals(TOOL_CALLS_URL, http.urls.get(0));
        JSONObject body = new JSONObject(http.bodies.get(0));
        assertEquals("example_read_tool", body.getString("name"));
        assertEquals(3, body.getJSONObject("arguments").getInt("limit"));
        assertEquals("alpha", body.getString("sessionName"));
    }

    @Test
    public void sendsNullSessionNameAsJsonNull() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient();
        forwarderFor(http).forward("example_read_tool", "{}", null);
        JSONObject body = new JSONObject(http.bodies.get(0));
        assertTrue(body.has("sessionName"));
        assertTrue(body.isNull("sessionName"));
    }

    @Test
    public void argumentsThatAreNotAJsonObjectBecomeEmptyObject() throws Exception {
        String[][] cases = {
            {"", "{}"},
            {"not json", "{}"},
            {"[1,2]", "{}"},
            {"\"text\"", "{}"},
            {"{\"query\":\"x\"}", "{\"query\":\"x\"}"},
        };
        for (String[] testCase : cases) {
            RecordingHttpClient http = new RecordingHttpClient();
            forwarderFor(http).forward("example_read_tool", testCase[0], "alpha");
            JSONObject body = new JSONObject(http.bodies.get(0));
            assertEquals(testCase[0], new JSONObject(testCase[1]).toString(),
                body.getJSONObject("arguments").toString());
        }
    }

    @Test
    public void returnsConsoleOutputVerbatim() throws Exception {
        String[] outputs = {"plain text", "{\"a\":1} tail", "", "  spaced \n lines  "};
        for (String output : outputs) {
            RecordingHttpClient http = new RecordingHttpClient();
            http.response = new LiveSessionHttpResponse(200, new JSONObject().put("output", output).toString());
            assertEquals(output, forwarderFor(http).forward("example_read_tool", "{}", "alpha"));
        }
    }

    @Test
    public void nonOkStatusFailsWithStatusInTheMessage() throws Exception {
        int[] statuses = {201, 400, 404, 500, 503};
        for (int status : statuses) {
            RecordingHttpClient http = new RecordingHttpClient();
            http.response = new LiveSessionHttpResponse(status, "{\"output\":\"ignored\"}");
            try {
                forwarderFor(http).forward("example_read_tool", "{}", "alpha");
                fail("status " + status);
            } catch (IOException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains(String.valueOf(status)));
            }
        }
    }

    @Test
    public void unreadableOrOutputlessBodyFails() throws Exception {
        String[] bodies = {"not json", "{}", "{\"output\":3}", "{\"output\":null}", "[]"};
        for (String body : bodies) {
            RecordingHttpClient http = new RecordingHttpClient();
            http.response = new LiveSessionHttpResponse(200, body);
            try {
                forwarderFor(http).forward("example_read_tool", "{}", "alpha");
                fail(body);
            } catch (IOException expected) {
                assertFalse(expected.getMessage().isEmpty());
            }
        }
    }

    @Test
    public void networkFailurePropagates() throws Exception {
        RecordingHttpClient http = new RecordingHttpClient();
        http.failure = new IOException("connection reset");
        try {
            forwarderFor(http).forward("example_read_tool", "{}", "alpha");
            fail("network failure");
        } catch (IOException expected) {
            assertEquals("connection reset", expected.getMessage());
        }
    }
}
