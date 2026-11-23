package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class VoiceConsoleEndpointUrlTest {

    @Test
    public void resolveDerivesTheLiveSessionsEndpointFromTheSessionDefinitionUrl() {
        String[][] inputAndExpected = {
            {"https://console.example.com/?k=abc", "https://console.example.com/api/live/sessions?k=abc"},
            {"http://192.168.0.10:8080/any/path?x=1&k=tok", "http://192.168.0.10:8080/api/live/sessions?k=tok"},
            {"https://console.example.com", "https://console.example.com/api/live/sessions"},
            {"  https://console.example.com/?k=abc  ", "https://console.example.com/api/live/sessions?k=abc"},
        };
        for (String[] testCase : inputAndExpected) {
            assertEquals("input " + testCase[0], testCase[1], VoiceConsoleEndpointUrl.resolve(testCase[0]));
        }
    }

    @Test
    public void resolveRejectsUnusableSessionDefinitionUrls() {
        String[] unusable = {null, "", "   ", "not a url", "/relative/path", "file:///sdcard/definition"};
        for (String input : unusable) {
            assertThrows("input " + input, IllegalArgumentException.class,
                () -> VoiceConsoleEndpointUrl.resolve(input));
        }
    }
}
