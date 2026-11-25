package com.termux.app.voiceconsole;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

public final class VoiceConsoleEndpointUrl {

    static final String LIVE_SESSIONS_PATH = "/api/live/sessions";

    private static final String ACCESS_TOKEN_PARAMETER_PREFIX = "k=";

    private VoiceConsoleEndpointUrl() {
    }

    public static VoiceConsoleEndpoint resolve(String sessionDefinitionUrl) {
        String trimmed = sessionDefinitionUrl == null ? "" : sessionDefinitionUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("the session definition URL is empty");
        }
        URL sessionDefinition;
        try {
            sessionDefinition = new URL(trimmed);
        } catch (MalformedURLException notAnAbsoluteUrl) {
            throw new IllegalArgumentException("the session definition URL is not an absolute URL", notAnAbsoluteUrl);
        }
        String authority = sessionDefinition.getAuthority();
        if (authority == null || authority.isEmpty()) {
            throw new IllegalArgumentException("the session definition URL has no authority");
        }
        String accessToken = accessTokenOf(sessionDefinition.getQuery());
        return new VoiceConsoleEndpoint(
            sessionDefinition.getProtocol() + "://" + authority + LIVE_SESSIONS_PATH, accessToken);
    }

    private static String accessTokenOf(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String parameter : query.split("&")) {
            if (parameter.startsWith(ACCESS_TOKEN_PARAMETER_PREFIX)) {
                return decode(parameter.substring(ACCESS_TOKEN_PARAMETER_PREFIX.length()));
            }
        }
        return null;
    }

    private static String decode(String encodedAccessToken) {
        try {
            String accessToken = URLDecoder.decode(encodedAccessToken, "UTF-8");
            if (!accessToken.isEmpty() && !isSendableAsHeaderValue(accessToken)) {
                throw new IllegalArgumentException("the access token has characters a request header cannot carry");
            }
            return accessToken;
        } catch (UnsupportedEncodingException unsupportedEncoding) {
            throw new IllegalArgumentException("the access token cannot be decoded", unsupportedEncoding);
        }
    }

    private static boolean isSendableAsHeaderValue(String accessToken) {
        for (int index = 0; index < accessToken.length(); index++) {
            char character = accessToken.charAt(index);
            if (character < 0x21 || character > 0x7E) {
                return false;
            }
        }
        return true;
    }
}
