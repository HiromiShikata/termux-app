package com.termux.app.sessiondefinition;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class SessionDefinitionPlainHttpRestore {
    private static final String REVERTED_SCHEME = "https";
    private static final int REVERTED_PORT = 9443;
    private static final String RESTORED_SCHEME = "http";
    private static final int RESTORED_PORT = 9980;

    public interface UrlStore {
        String get();

        void set(String url);
    }

    private SessionDefinitionPlainHttpRestore() {
    }

    public static String restore(String url) {
        if (url == null) return null;
        try {
            URI uri = new URI(url);
            if (!REVERTED_SCHEME.equalsIgnoreCase(uri.getScheme())) return url;
            if (uri.getRawAuthority() == null || uri.getPort() != REVERTED_PORT) return url;
            String userInfo = uri.getRawUserInfo();
            String host = uri.getHost();
            StringBuilder restored = new StringBuilder(RESTORED_SCHEME).append("://");
            if (userInfo != null) restored.append(userInfo).append('@');
            restored.append(host).append(':').append(RESTORED_PORT);
            if (uri.getRawPath() != null) restored.append(uri.getRawPath());
            if (uri.getRawQuery() != null) restored.append('?').append(uri.getRawQuery());
            if (uri.getRawFragment() != null) restored.append('#').append(uri.getRawFragment());
            return restored.toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    public static boolean shouldPersist(String urlBeforeRead, String restoredUrl, String urlAfterRead) {
        return restoredUrl != null
            && !Objects.equals(restoredUrl, urlBeforeRead)
            && Objects.equals(urlAfterRead, urlBeforeRead);
    }

    public static boolean restoreStore(UrlStore store) {
        String before = store.get();
        String restored = restore(before);
        if (!shouldPersist(before, restored, store.get())) return false;
        store.set(restored);
        return true;
    }
}
