package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class BrowserClipboardUrlOffer {

    private static final String SECURE_SCHEME = "https://";

    private static final String PLAIN_SCHEME = "http://";

    private final boolean mOffered;

    private final String mUrl;

    private BrowserClipboardUrlOffer(boolean offered, String url) {
        mOffered = offered;
        mUrl = url;
    }

    @NonNull
    public static BrowserClipboardUrlOffer of(@Nullable CharSequence clipboardText) {
        if (clipboardText == null) return nothingToOffer();

        String trimmed = clipboardText.toString().trim();
        if (!startsWithAWebScheme(trimmed)) return nothingToOffer();
        if (holdsWhitespaceInside(trimmed)) return nothingToOffer();
        if (hostOf(trimmed).isEmpty()) return nothingToOffer();

        return new BrowserClipboardUrlOffer(true, trimmed);
    }

    public boolean isOffered() {
        return mOffered;
    }

    @NonNull
    public String getUrl() {
        if (!mOffered) {
            throw new IllegalStateException("the clipboard holds no web address, so there is none to open");
        }
        return mUrl;
    }

    @NonNull
    public String getHost() {
        if (!mOffered) {
            throw new IllegalStateException("the clipboard holds no web address, so there is no site to name");
        }
        return hostOf(mUrl);
    }

    private static BrowserClipboardUrlOffer nothingToOffer() {
        return new BrowserClipboardUrlOffer(false, null);
    }

    private static boolean startsWithAWebScheme(@NonNull String text) {
        String lowerCased = text.toLowerCase(Locale.ROOT);
        return lowerCased.startsWith(SECURE_SCHEME) || lowerCased.startsWith(PLAIN_SCHEME);
    }

    private static boolean holdsWhitespaceInside(@NonNull String text) {
        for (int index = 0; index < text.length(); index++) {
            if (Character.isWhitespace(text.charAt(index))) return true;
        }
        return false;
    }

    @NonNull
    private static String hostOf(@NonNull String url) {
        String behindTheScheme = url.substring(schemeLengthOf(url));
        int authorityEnd = behindTheScheme.length();
        for (int index = 0; index < behindTheScheme.length(); index++) {
            char character = behindTheScheme.charAt(index);
            if (character == '/' || character == '?' || character == '#') {
                authorityEnd = index;
                break;
            }
        }
        String authority = behindTheScheme.substring(0, authorityEnd);
        int credentialsEnd = authority.lastIndexOf('@');
        if (credentialsEnd >= 0) authority = authority.substring(credentialsEnd + 1);
        if (authority.startsWith("[")) {
            int addressLiteralEnd = authority.indexOf(']');
            if (addressLiteralEnd < 0) return "";
            return authority.substring(0, addressLiteralEnd + 1);
        }
        int portStart = authority.indexOf(':');
        if (portStart >= 0) authority = authority.substring(0, portStart);
        return authority;
    }

    private static int schemeLengthOf(@NonNull String url) {
        return url.toLowerCase(Locale.ROOT).startsWith(SECURE_SCHEME)
            ? SECURE_SCHEME.length() : PLAIN_SCHEME.length();
    }
}
