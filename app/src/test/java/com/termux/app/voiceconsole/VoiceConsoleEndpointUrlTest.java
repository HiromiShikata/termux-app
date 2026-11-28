package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class VoiceConsoleEndpointUrlTest {

    @Test
    public void resolveDerivesTheLiveSessionsEndpointAndAccessToken() {
        String[][] inputAndExpected = {
            {"https://console.example.com/definition?k=abc", "https://console.example.com/api/live/sessions", "abc"},
            {"http://console.example.com:8080/any/path?k=a%2Bb", "http://console.example.com:8080/api/live/sessions", "a+b"},
            {"https://console.example.com/definition?k=a+b", "https://console.example.com/api/live/sessions", "a b"},
            {"https://console.example.com/definition", "https://console.example.com/api/live/sessions", null},
            {"https://console.example.com/definition?k=", "https://console.example.com/api/live/sessions", ""},
            {"  http://console.example.com:8080/definition?k=test-token  ", "http://console.example.com:8080/api/live/sessions", "test-token"},
            {"https://console.example.com:9443/in-tmux-by-human/index.v3.json?k=test-token", "https://console.example.com:9443/api/live/sessions", "test-token"},
            {"http://console.example.com:9980/in-tmux-by-human/index.v3.json?k=test-token", "http://console.example.com:9980/api/live/sessions", "test-token"},
        };
        for (String[] testCase : inputAndExpected) {
            VoiceConsoleEndpoint endpoint = VoiceConsoleEndpointUrl.resolve(testCase[0]);
            assertEquals("input " + testCase[0], testCase[1], endpoint.getUrl());
            if (testCase[2] == null) {
                org.junit.Assert.assertNull("input " + testCase[0], endpoint.getAccessToken());
            } else {
                assertEquals("input " + testCase[0], testCase[2], endpoint.getAccessToken());
            }
        }
    }

    @Test
    public void resolveRejectsUnusableSessionDefinitionUrls() {
        String[] unusable = {null, "", "   ", "not a url", "/relative/path", "file:///sdcard/definition", "https:///definition"};
        for (String input : unusable) {
            assertThrows("input " + input, IllegalArgumentException.class,
                () -> VoiceConsoleEndpointUrl.resolve(input));
        }
    }

    @Test
    public void resolveRejectsMalformedAccessTokenEscapes() {
        assertThrows(IllegalArgumentException.class,
            () -> VoiceConsoleEndpointUrl.resolve("https://console.example.com/definition?k=%ZZ"));
    }

    @Test
    public void resolveRejectsAccessTokensWithCharactersUnavailableToRequestHeaders() {
        String[] unusableAccessTokens = {
            "https://console.example.com/definition?k=a%0Ab",
            "https://console.example.com/definition?k=%E3%81%82",
        };
        for (String input : unusableAccessTokens) {
            assertThrows("input " + input, IllegalArgumentException.class,
                () -> VoiceConsoleEndpointUrl.resolve(input));
        }
    }
}
