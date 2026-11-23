package com.termux.app.voicepatrol;

import java.io.IOException;

public interface LiveToolCallHttpClient {

    LiveSessionHttpResponse post(String endpointUrl, String jsonBody) throws IOException;
}
