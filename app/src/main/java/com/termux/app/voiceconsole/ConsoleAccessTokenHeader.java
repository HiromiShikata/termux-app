package com.termux.app.voiceconsole;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Map;

public final class ConsoleAccessTokenHeader {

    public static final String NAME = "x-pv-token";

    private ConsoleAccessTokenHeader() {
    }

    public static Map<String, String> requestHeadersFor(@Nullable String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(NAME, accessToken);
    }
}
