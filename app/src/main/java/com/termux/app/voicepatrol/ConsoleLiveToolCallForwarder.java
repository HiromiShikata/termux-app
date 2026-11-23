package com.termux.app.voicepatrol;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public final class ConsoleLiveToolCallForwarder implements LiveToolCallForwarder {

    private static final int STATUS_OK = 200;
    private static final String OUTPUT_PROPERTY = "output";

    private final LiveToolCallHttpClient mClient;
    private final String mToolCallsUrl;

    public ConsoleLiveToolCallForwarder(LiveToolCallHttpClient client, String toolCallsUrl) {
        mClient = client;
        mToolCallsUrl = toolCallsUrl;
    }

    @Override
    public String forward(String toolName, String argumentsJson, String sessionName) throws IOException {
        LiveSessionHttpResponse response =
            mClient.post(mToolCallsUrl, requestBody(toolName, argumentsJson, sessionName));
        if (response.getStatusCode() != STATUS_OK) {
            throw new IOException("the console answered HTTP " + response.getStatusCode());
        }
        return outputOf(response.getBody());
    }

    static String requestBody(String toolName, String argumentsJson, String sessionName) {
        try {
            return new JSONObject()
                .put("name", toolName)
                .put("arguments", argumentsObjectOf(argumentsJson))
                .put("sessionName", sessionName == null ? JSONObject.NULL : sessionName)
                .toString();
        } catch (JSONException impossibleForStringValues) {
            throw new IllegalStateException(impossibleForStringValues);
        }
    }

    private static JSONObject argumentsObjectOf(String argumentsJson) {
        if (argumentsJson == null) {
            return new JSONObject();
        }
        try {
            return new JSONObject(argumentsJson);
        } catch (JSONException notAnObject) {
            return new JSONObject();
        }
    }

    static String outputOf(String responseBody) throws IOException {
        Object output;
        try {
            output = new JSONObject(responseBody).opt(OUTPUT_PROPERTY);
        } catch (JSONException notAnObject) {
            throw new IOException("the console answer is not a JSON object", notAnObject);
        }
        if (!(output instanceof String)) {
            throw new IOException("the console answer has no string output");
        }
        return (String) output;
    }
}
