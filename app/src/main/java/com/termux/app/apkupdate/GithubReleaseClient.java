package com.termux.app.apkupdate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class GithubReleaseClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 15000;

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    public String fetchLatestReleaseJson(String url) throws IOException {
        return fetch(url, "application/vnd.github+json");
    }

    public String fetchReleasesListJson(String url) throws IOException {
        return fetch(url, "application/vnd.github+json");
    }

    /**
     * Fetches the GitHub Releases Atom feed. The Atom feed is served from the github.com web host,
     * not {@code api.github.com}, so it is not subject to the unauthenticated REST rate limit and is
     * usable as a fallback when {@link #fetchLatestReleaseJson(String)} answers with a rate limit.
     */
    public String fetchReleasesAtomFeed(String url) throws IOException {
        return fetch(url, "application/atom+xml");
    }

    public String fetchReleaseSha256Sums(String url) throws IOException {
        return fetch(url, "text/plain");
    }

    private String fetch(String url, String acceptHeader) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        if (!(urlConnection instanceof HttpURLConnection)) {
            throw new IOException("Unsupported protocol for releases URL: " + url);
        }
        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", acceptHeader);

            int responseCode = connection.getResponseCode();
            if (isRateLimited(connection, responseCode)) {
                throw new GithubRateLimitedException(
                    "GitHub rate limit reached (HTTP " + responseCode + ") for " + url);
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code " + responseCode + " for " + url);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return readAll(inputStream);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * A response is a rate-limit response when GitHub answers 403 or 429 with the remaining
     * rate-limit budget exhausted. GitHub returns 403 for the unauthenticated primary limit and 429
     * for secondary limits; both carry {@code X-RateLimit-Remaining: 0}. Some 403 responses are not
     * rate limits (for example a private resource), so the header is required for the 403 case; a 429
     * is always a rate limit even if the header is absent.
     */
    private boolean isRateLimited(HttpURLConnection connection, int responseCode) {
        if (responseCode == HTTP_TOO_MANY_REQUESTS) {
            return true;
        }
        if (responseCode != HttpURLConnection.HTTP_FORBIDDEN) {
            return false;
        }
        String remaining = connection.getHeaderField("X-RateLimit-Remaining");
        return "0".equals(remaining);
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
