package com.termux.app.sessiondefinition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class HttpSessionDefinitionDocumentFetcher implements SessionDefinitionDocumentFetcher {

    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 15000;

    @Override
    public String fetch(String url) throws IOException {
        URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException malformed) {
            throw new MalformedURLException("Malformed session definition URL: " + redactAccessToken(url));
        }
        URLConnection urlConnection = parsedUrl.openConnection();
        if (!(urlConnection instanceof HttpURLConnection)) {
            throw new IOException(unsupportedProtocolMessage(url));
        }
        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(unexpectedResponseMessage(responseCode, url));
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return readAll(inputStream);
            }
        } finally {
            connection.disconnect();
        }
    }

    static String redactAccessToken(String url) {
        return url.replaceAll("([?&]k=)[^&]*", "$1<redacted>");
    }

    static String unsupportedProtocolMessage(String url) {
        return "Unsupported protocol for session definition URL: " + redactAccessToken(url);
    }

    static String unexpectedResponseMessage(int responseCode, String url) {
        return "Unexpected HTTP response code " + responseCode + " for " + redactAccessToken(url);
    }

    private static String readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
