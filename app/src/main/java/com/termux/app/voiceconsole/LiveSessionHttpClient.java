package com.termux.app.voiceconsole;

import java.io.IOException;

public interface LiveSessionHttpClient {

    LiveSessionHttpResponse post(String endpointUrl, String offerSdp) throws IOException;
}
