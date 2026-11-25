package com.termux.app.voiceconsole;

import java.io.IOException;

public interface LiveSessionHttpClient {

    LiveSessionHttpResponse post(VoiceConsoleEndpoint endpoint, String offerSdp) throws IOException;
}
