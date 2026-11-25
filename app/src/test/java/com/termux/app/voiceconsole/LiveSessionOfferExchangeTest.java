package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LiveSessionOfferExchangeTest {
    static final class Client implements LiveSessionHttpClient {
        VoiceConsoleEndpoint endpoint;
        String offer;
        LiveSessionHttpResponse response;
        IOException failure;

        @Override
        public LiveSessionHttpResponse post(VoiceConsoleEndpoint value, String offerSdp) throws IOException {
            endpoint = value;
            offer = offerSdp;
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }

    @Test
    public void delegatesEndpointAndOfferUnchangedAndMapsAnswer() {
        Client client = new Client();
        client.response = new LiveSessionHttpResponse(200, "answer");
        VoiceConsoleEndpoint endpoint = new VoiceConsoleEndpoint(
            "https://console.example.com/api/live/sessions", "test-token");

        LiveSessionAnswer answer = new LiveSessionOfferExchange(client).exchange(endpoint, "offer");

        assertTrue(answer.isAccepted());
        assertEquals(endpoint, client.endpoint);
        assertEquals("https://console.example.com/api/live/sessions", client.endpoint.getUrl());
        assertEquals("test-token", client.endpoint.getAccessToken());
        assertEquals("offer", client.offer);
        assertEquals("answer", answer.getSdp());
    }

    @Test
    public void ioExceptionBecomesNetworkError() {
        Client client = new Client();
        client.failure = new IOException();

        assertEquals("network_error", new LiveSessionOfferExchange(client).exchange(
            new VoiceConsoleEndpoint("https://console.example.com/api/live/sessions", "test-token"), "offer")
            .getFailureReason());
    }

    @Test
    public void serverFailuresKeepServerReasons() {
        String[][] responses = {{"503", "{\"error\":\"openai_api_key_missing\"}", "openai_api_key_missing"},
            {"502", "{\"error\":\"openai_live_session_failed\"}", "openai_live_session_failed"},
            {"400", "bad", "HTTP 400"}, {"401", "", "HTTP 401"}};
        for (String[] response : responses) {
            Client client = new Client();
            client.response = new LiveSessionHttpResponse(Integer.parseInt(response[0]), response[1]);
            assertEquals(response[2], new LiveSessionOfferExchange(client).exchange(
                new VoiceConsoleEndpoint("https://console.example.com/api/live/sessions", null), "offer")
                .getFailureReason());
        }
    }
}
