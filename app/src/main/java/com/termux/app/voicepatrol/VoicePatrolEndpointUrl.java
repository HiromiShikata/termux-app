package com.termux.app.voicepatrol;

import java.net.MalformedURLException;
import java.net.URL;

public final class VoicePatrolEndpointUrl {

    static final String LIVE_SESSIONS_PATH = "/api/live/sessions";

    private static final String ACCESS_TOKEN_PARAMETER_PREFIX = "k=";

    private VoicePatrolEndpointUrl() {
    }

    public static String resolve(String sessionDefinitionUrl) {
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
        return sessionDefinition.getProtocol() + "://" + authority + LIVE_SESSIONS_PATH
            + (accessToken == null ? "" : "?k=" + accessToken);
    }

    private static String accessTokenOf(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String parameter : query.split("&")) {
            if (parameter.startsWith(ACCESS_TOKEN_PARAMETER_PREFIX)) {
                return parameter.substring(ACCESS_TOKEN_PARAMETER_PREFIX.length());
            }
        }
        return null;
    }
}
