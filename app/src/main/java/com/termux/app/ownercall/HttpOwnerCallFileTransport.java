package com.termux.app.ownercall;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class HttpOwnerCallFileTransport implements OwnerCallFileTransport {

    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 15000;
    private static final char QUERY_START = '?';

    @NonNull
    @Override
    public String fetch(@NonNull String url) throws IOException {
        HttpURLConnection connection = openConnection(url, "GET");
        try {
            int responseCode = responseCodeOf(connection, url);
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return "";
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code " + responseCode + " for "
                    + OwnerCallFileUrl.describe(url));
            }
            return bodyOf(connection, url);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void delete(@NonNull String url) throws IOException {
        HttpURLConnection connection = openConnection(url, "DELETE");
        try {
            int responseCode = responseCodeOf(connection, url);
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return;
            }
            if (responseCode < HttpURLConnection.HTTP_OK
                || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                throw new IOException("Unexpected HTTP response code " + responseCode + " for "
                    + OwnerCallFileUrl.describe(url));
            }
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private static HttpURLConnection openConnection(@NonNull String url, @NonNull String method)
        throws IOException {
        URLConnection urlConnection;
        try {
            urlConnection = new URL(url).openConnection();
        } catch (IOException unusableUrl) {
            throw withoutTheAccessToken(unusableUrl, url);
        }
        if (!(urlConnection instanceof HttpURLConnection)) {
            throw new IOException("Unsupported protocol for owner call file URL: "
                + OwnerCallFileUrl.describe(url));
        }
        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestMethod(method);
        return connection;
    }

    private static int responseCodeOf(@NonNull HttpURLConnection connection, @NonNull String url)
        throws IOException {
        try {
            return connection.getResponseCode();
        } catch (IOException unreachableServer) {
            throw withoutTheAccessToken(unreachableServer, url);
        }
    }

    @NonNull
    private static String bodyOf(@NonNull HttpURLConnection connection, @NonNull String url)
        throws IOException {
        try (InputStream inputStream = connection.getInputStream()) {
            return readAll(inputStream);
        } catch (IOException unreadableBody) {
            throw withoutTheAccessToken(unreadableBody, url);
        }
    }

    @NonNull
    private static IOException withoutTheAccessToken(@NonNull IOException failure,
                                                     @NonNull String url) {
        String message = failure.getClass().getName() + ": " + failure.getMessage();
        int queryStart = url.indexOf(QUERY_START);
        if (queryStart < 0) {
            return new IOException(message);
        }
        String query = url.substring(queryStart);
        return new IOException(message.replace(query, "").replace(query.substring(1), ""));
    }

    @NonNull
    private static String readAll(@NonNull InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
