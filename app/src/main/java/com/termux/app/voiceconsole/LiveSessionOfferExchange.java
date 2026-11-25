package com.termux.app.voiceconsole;

import java.io.IOException;

public final class LiveSessionOfferExchange {

    static final String NETWORK_ERROR_REASON = "network_error";

    private final LiveSessionHttpClient mClient;

    public LiveSessionOfferExchange(LiveSessionHttpClient client) {
        mClient = client;
    }

    public LiveSessionAnswer exchange(VoiceConsoleEndpoint endpoint, String offerSdp) {
        try {
            LiveSessionHttpResponse response = mClient.post(endpoint, offerSdp);
            return LiveSessionAnswer.fromHttpResponse(response.getStatusCode(), response.getBody());
        } catch (IOException networkFailure) {
            return LiveSessionAnswer.failed(NETWORK_ERROR_REASON);
        }
    }
}
