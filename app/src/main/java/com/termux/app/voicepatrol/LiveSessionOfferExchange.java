package com.termux.app.voicepatrol;

import java.io.IOException;

public final class LiveSessionOfferExchange {

    static final String NETWORK_ERROR_REASON = "network_error";

    private final LiveSessionHttpClient mClient;

    public LiveSessionOfferExchange(LiveSessionHttpClient client) {
        mClient = client;
    }

    public LiveSessionAnswer exchange(String endpointUrl, String offerSdp) {
        try {
            LiveSessionHttpResponse response = mClient.post(endpointUrl, offerSdp);
            return LiveSessionAnswer.fromHttpResponse(response.getStatusCode(), response.getBody());
        } catch (IOException networkFailure) {
            return LiveSessionAnswer.failed(NETWORK_ERROR_REASON);
        }
    }
}
