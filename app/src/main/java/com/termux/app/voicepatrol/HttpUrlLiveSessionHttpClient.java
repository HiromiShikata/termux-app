package com.termux.app.voicepatrol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpUrlLiveSessionHttpClient implements LiveSessionHttpClient {

    private static final String SDP_CONTENT_TYPE = "application/sdp";
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    @Override
    public LiveSessionHttpResponse post(String endpointUrl, String offerSdp) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty("Content-Type", SDP_CONTENT_TYPE);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(offerSdp.getBytes(StandardCharsets.UTF_8));
            }
            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new LiveSessionHttpResponse(statusCode, stream == null ? "" : readFully(stream));
        } finally {
            connection.disconnect();
        }
    }

    private static String readFully(InputStream stream) throws IOException {
        try (InputStream input = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
}
