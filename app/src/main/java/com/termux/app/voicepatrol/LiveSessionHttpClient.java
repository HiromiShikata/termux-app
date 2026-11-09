package com.termux.app.voicepatrol;

import java.io.IOException;

public interface LiveSessionHttpClient {

    LiveSessionHttpResponse post(String endpointUrl, String offerSdp) throws IOException;
}
