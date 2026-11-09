package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class VoicePatrolEndpointUrlTest {

    @Test
    public void derivesSchemeAuthorityAndTokenFromSessionDefinitionUrl() {
        String[][] cases = {
            {"https://pc.example.com:8443/owner-calls/session.json?k=SECRET",
                "https://pc.example.com:8443/api/live/sessions?k=SECRET"},
            {"http://192.168.0.5:7777/a/b/c?k=abc123",
                "http://192.168.0.5:7777/api/live/sessions?k=abc123"},
            {"https://pc.example.com/definition?k=t",
                "https://pc.example.com/api/live/sessions?k=t"},
            {"https://pc.example.com/x/y.json",
                "https://pc.example.com/api/live/sessions"},
            {"  https://pc.example.com/x/y.json?k=tok  ",
                "https://pc.example.com/api/live/sessions?k=tok"},
        };
        for (String[] testCase : cases) {
            assertEquals(testCase[0], testCase[1], VoicePatrolEndpointUrl.resolve(testCase[0]));
        }
    }

    @Test
    public void rejectsUnusableSessionDefinitionUrls() {
        String[] unusable = {"", "   ", "not a url", "/relative/path?k=t"};
        for (String value : unusable) {
            try {
                VoicePatrolEndpointUrl.resolve(value);
                fail("expected IllegalArgumentException for: " + value);
            } catch (IllegalArgumentException expected) {
                assertEquals(true, expected.getMessage() != null && !expected.getMessage().isEmpty());
            }
        }
    }
}
