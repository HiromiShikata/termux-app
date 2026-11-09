package com.termux.app.voicepatrol;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class LiveEvents {

    static final int MAX_CONTEXT_CHUNK_CODE_POINTS = 400;

    private static final String RESPONSE_EVENT_ENVELOPE = "response.event";

    private LiveEvents() {
    }

    public static LiveServerEvent parseServerEvent(String eventJson) {
        JSONObject root = parseObject(eventJson);
        JSONObject event = root;
        if (RESPONSE_EVENT_ENVELOPE.equals(root.optString("type"))) {
            event = root.optJSONObject("event");
            if (event == null) {
                throw new IllegalArgumentException("response.event envelope has no event object");
            }
        }
        String type = event.optString("type");
        if ("session.started".equals(type)) {
            return LiveServerEvent.ofKind(LiveServerEventKind.SESSION_STARTED);
        }
        if ("session.closed".equals(type)) {
            return LiveServerEvent.ofKind(LiveServerEventKind.SESSION_CLOSED);
        }
        if ("response.output_item.done".equals(type)) {
            JSONObject item = event.optJSONObject("item");
            if (item != null && "function_call".equals(item.optString("type"))) {
                String callId = item.optString("call_id");
                if (callId.isEmpty()) {
                    throw new IllegalArgumentException("function_call item has no call_id");
                }
                return LiveServerEvent.functionCall(
                    callId, item.optString("name"), item.optString("arguments", "{}"));
            }
        }
        return LiveServerEvent.ofKind(LiveServerEventKind.OTHER);
    }

    public static List<String> sessionThinkingAppends(String eventIdPrefix, String content) {
        List<String> events = new ArrayList<>();
        int[] codePoints = content.codePoints().toArray();
        int chunkIndex = 0;
        for (int start = 0; start < codePoints.length; start += MAX_CONTEXT_CHUNK_CODE_POINTS) {
            int end = Math.min(codePoints.length, start + MAX_CONTEXT_CHUNK_CODE_POINTS);
            String chunk = new String(codePoints, start, end - start);
            events.add(sessionThinkingAppend(eventIdPrefix + "-" + chunkIndex, chunk));
            chunkIndex++;
        }
        return events;
    }

    public static String functionCallOutput(String eventId, String callId, String output) {
        try {
            JSONObject item = new JSONObject()
                .put("type", "function_call_output")
                .put("call_id", callId)
                .put("output", output);
            return new JSONObject()
                .put("type", "response.item.create")
                .put("event_id", eventId)
                .put("item", item)
                .toString();
        } catch (JSONException impossibleForStringValues) {
            throw new IllegalStateException(impossibleForStringValues);
        }
    }

    public static String responseCreate(String eventId) {
        return typeAndEventId("response.create", eventId);
    }

    public static String sessionClose(String eventId) {
        return typeAndEventId("session.close", eventId);
    }

    private static String sessionThinkingAppend(String eventId, String content) {
        try {
            return new JSONObject()
                .put("type", "session.thinking.append")
                .put("event_id", eventId)
                .put("delegation_id", JSONObject.NULL)
                .put("content", content)
                .toString();
        } catch (JSONException impossibleForStringValues) {
            throw new IllegalStateException(impossibleForStringValues);
        }
    }

    private static String typeAndEventId(String type, String eventId) {
        try {
            return new JSONObject().put("type", type).put("event_id", eventId).toString();
        } catch (JSONException impossibleForStringValues) {
            throw new IllegalStateException(impossibleForStringValues);
        }
    }

    private static JSONObject parseObject(String eventJson) {
        try {
            return new JSONObject(eventJson);
        } catch (JSONException notAnObject) {
            throw new IllegalArgumentException("live event is not a JSON object: " + notAnObject.getMessage(),
                notAnObject);
        }
    }
}
