package com.termux.app.voicepatrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpUrlLiveToolCallHttpClient implements LiveToolCallHttpClient {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final int TIMEOUT_MILLIS = 15_000;

    @Override
    public LiveSessionHttpResponse post(String endpointUrl, String jsonBody) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new LiveSessionHttpResponse(statusCode,
                stream == null ? "" : HttpUrlLiveSessionHttpClient.readFully(stream));
        } finally {
            connection.disconnect();
        }
    }
}
