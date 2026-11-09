package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class LiveEventsTest {

    @Test
    public void parsesFunctionCallFromNestedResponseEventEnvelope() {
        LiveServerEvent event = LiveEvents.parseServerEvent(
            "{\"type\":\"response.event\",\"delegation_id\":\"item_1\",\"event\":{"
                + "\"type\":\"response.output_item.done\",\"item\":{\"type\":\"function_call\","
                + "\"call_id\":\"call_9\",\"name\":\"session_skip\",\"arguments\":\"{}\"}}}");

        assertEquals(LiveServerEventKind.FUNCTION_CALL, event.getKind());
        assertEquals("call_9", event.getCallId());
        assertEquals("session_skip", event.getName());
        assertEquals("{}", event.getArguments());
    }

    @Test
    public void parsesSessionLifecycleEvents() {
        assertEquals(LiveServerEventKind.SESSION_STARTED,
            LiveEvents.parseServerEvent("{\"type\":\"session.started\"}").getKind());
        assertEquals(LiveServerEventKind.SESSION_CLOSED,
            LiveEvents.parseServerEvent("{\"type\":\"session.closed\"}").getKind());
    }

    @Test
    public void otherEventsAreClassifiedAsOther() {
        assertEquals(LiveServerEventKind.OTHER,
            LiveEvents.parseServerEvent("{\"type\":\"response.event\",\"event\":{\"type\":\"response.created\"}}")
                .getKind());
    }

    @Test
    public void functionCallWithoutCallIdIsRejected() {
        try {
            LiveEvents.parseServerEvent("{\"type\":\"response.event\",\"event\":{"
                + "\"type\":\"response.output_item.done\",\"item\":{\"type\":\"function_call\","
                + "\"name\":\"session_skip\",\"arguments\":\"{}\"}}}");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("call_id"));
        }
    }

    @Test
    public void invalidJsonIsRejectedWithIllegalArgumentException() {
        try {
            LiveEvents.parseServerEvent("not json");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }

    @Test
    public void thinkingAppendHasDelegationIdNullAndPlainContent() throws Exception {
        List<String> events = LiveEvents.sessionThinkingAppends("ctx", "short body");

        assertEquals(1, events.size());
        JSONObject json = new JSONObject(events.get(0));
        assertEquals("session.thinking.append", json.getString("type"));
        assertEquals("ctx-0", json.getString("event_id"));
        assertTrue(json.isNull("delegation_id"));
        assertEquals("short body", json.getString("content"));
    }

    @Test
    public void longContextIsSplitIntoChunksOfAtMostFourHundredCodePoints() throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            builder.append('あ');
        }

        List<String> events = LiveEvents.sessionThinkingAppends("ctx", builder.toString());

        assertEquals(3, events.size());
        StringBuilder rejoined = new StringBuilder();
        for (String event : events) {
            String content = new JSONObject(event).getString("content");
            assertTrue(content.codePointCount(0, content.length()) <= 400);
            rejoined.append(content);
        }
        assertEquals(builder.toString(), rejoined.toString());
    }

    @Test
    public void functionCallOutputMatchesCallIdAsResponseItemCreate() throws Exception {
        JSONObject json = new JSONObject(LiveEvents.functionCallOutput("out_1", "call_9", "{\"status\":\"sent\"}"));

        assertEquals("response.item.create", json.getString("type"));
        assertEquals("out_1", json.getString("event_id"));
        JSONObject item = json.getJSONObject("item");
        assertEquals("function_call_output", item.getString("type"));
        assertEquals("call_9", item.getString("call_id"));
        assertEquals("{\"status\":\"sent\"}", item.getString("output"));
    }

    @Test
    public void responseCreateAndSessionCloseHaveTheirTypes() throws Exception {
        assertEquals("response.create",
            new JSONObject(LiveEvents.responseCreate("cont_1")).getString("type"));
        assertEquals("session.close",
            new JSONObject(LiveEvents.sessionClose("close_1")).getString("type"));
    }
}
