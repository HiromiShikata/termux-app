package com.termux.app.sessiondefinition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

public final class SessionDefinitionHttpsUpgrade {
    private static final int MAX_RESPONSE_BYTES = 4096;

    private SessionDefinitionHttpsUpgrade() {
    }

    public static boolean shouldPersist(String urlBeforeFetch, String upgradedUrl, String urlAfterFetch) {
        return upgradedUrl != null
            && !Objects.equals(upgradedUrl, urlBeforeFetch)
            && Objects.equals(urlAfterFetch, urlBeforeFetch);
    }

    public static boolean isPlainHttp(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            return "http".equalsIgnoreCase(uri.getScheme());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String endpointFor(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getRawAuthority() == null) throw new IllegalArgumentException("URL has no authority");
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/api/console/https-endpoint";
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    public static String rewrite(String url, String httpsBaseUrl) {
        try {
            URI original = new URI(url);
            URI base = new URI(httpsBaseUrl);
            StringBuilder rewritten = new StringBuilder(httpsBaseUrl);
            String basePath = base.getRawPath();
            if (basePath != null && basePath.endsWith("/")) {
                rewritten.setLength(rewritten.length() - 1);
            }
            String path = original.getRawPath();
            if (path != null) rewritten.append(path);
            String query = original.getRawQuery();
            if (query != null) rewritten.append('?').append(query);
            String fragment = original.getRawFragment();
            if (fragment != null) rewritten.append('#').append(fragment);
            return rewritten.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
    }

    public static String parseHttpsBaseUrl(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) return null;
        JsonStringParser parser = new JsonStringParser(responseBody);
        return parser.parse();
    }

    public static String validatedHttpsBaseUrl(String originalUrl, String httpsBaseUrl) {
        if (originalUrl == null || httpsBaseUrl == null) return null;
        try {
            URI original = new URI(originalUrl);
            URI base = new URI(httpsBaseUrl);
            if (!"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null
                || original.getHost() == null || !base.getHost().equalsIgnoreCase(original.getHost())
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || !(base.getPath().isEmpty() || "/".equals(base.getPath()))) return null;
            return httpsBaseUrl;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public interface Fetcher {
        String fetchBody(String endpointUrl) throws IOException;
    }

    public interface UrlStore {
        String get();
        void set(String url);
    }

    public static final Fetcher PRODUCTION_FETCHER = endpointUrl -> {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpointUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int remaining = MAX_RESPONSE_BYTES;
                while (remaining > 0) {
                    int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
                    if (count < 0) break;
                    output.write(buffer, 0, count);
                    remaining -= count;
                }
                return new String(output.toByteArray(), "UTF-8");
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    };

    public static String resolve(String url, Fetcher fetcher) {
        if (!isPlainHttp(url)) return url;
        try {
            String validated = validatedHttpsBaseUrl(url, parseHttpsBaseUrl(fetcher.fetchBody(endpointFor(url))));
            if (validated == null) return url;
            String probeBody = fetcher.fetchBody(validated + "/api/console/https-endpoint");
            return Objects.equals(parseHttpsBaseUrl(probeBody), validated) ? rewrite(url, validated) : url;
        } catch (IOException | RuntimeException e) {
            return url;
        }
    }

    public static boolean upgradeStore(UrlStore store, Fetcher fetcher) {
        String url = store.get();
        String upgraded = resolve(url, fetcher);
        if (shouldPersist(url, upgraded, store.get())) {
            store.set(upgraded);
            return true;
        }
        return false;
    }

    private static final class JsonStringParser {
        private final String text;
        private int position;

        JsonStringParser(String text) {
            this.text = text;
        }

        String parse() {
            skipWhitespace();
            if (!consume('{')) return null;
            skipWhitespace();
            if (consume('}')) return null;
            String key = parseString();
            if (!"httpsBaseUrl".equals(key)) return null;
            skipWhitespace();
            if (!consume(':')) return null;
            String value = parseString();
            if (value == null) return null;
            skipWhitespace();
            if (!consume('}')) return null;
            skipWhitespace();
            return position == text.length() ? value : null;
        }

        private String parseString() {
            if (!consume('"')) return null;
            StringBuilder value = new StringBuilder();
            while (position < text.length()) {
                char character = text.charAt(position++);
                if (character == '"') return value.toString();
                if (character < 0x20) return null;
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (position >= text.length()) return null;
                char escaped = text.charAt(position++);
                switch (escaped) {
                    case '"': case '\\': case '/': value.append(escaped); break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case 'u':
                        if (position + 4 > text.length()) return null;
                        try {
                            value.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                        } catch (NumberFormatException e) {
                            return null;
                        }
                        position += 4;
                        break;
                    default: return null;
                }
            }
            return null;
        }

        private void skipWhitespace() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
        }

        private boolean consume(char expected) {
            if (position < text.length() && text.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }
    }
}
