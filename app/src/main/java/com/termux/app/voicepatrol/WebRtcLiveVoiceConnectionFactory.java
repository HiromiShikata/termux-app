package com.termux.app.voicepatrol;

import android.content.Context;

import org.webrtc.PeerConnectionFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebRtcLiveVoiceConnectionFactory implements LiveVoiceConnectionFactory {

    private final Context mContext;
    private final ExecutorService mHttpExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread worker = new Thread(runnable, "termux-voice-patrol-http");
        worker.setDaemon(true);
        return worker;
    });
    private final LiveSessionOfferExchange mOfferExchange =
        new LiveSessionOfferExchange(new HttpUrlLiveSessionHttpClient());
    private PeerConnectionFactory mPeerConnectionFactory;

    public WebRtcLiveVoiceConnectionFactory(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public LiveVoiceConnection open(String endpointUrl, LiveVoiceConnectionListener listener) {
        WebRtcLiveVoiceConnection connection = new WebRtcLiveVoiceConnection(
            peerConnectionFactory(), mHttpExecutor, mOfferExchange, endpointUrl, listener);
        connection.start();
        return connection;
    }

    private PeerConnectionFactory peerConnectionFactory() {
        if (mPeerConnectionFactory == null) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(mContext).createInitializationOptions());
            mPeerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
        }
        return mPeerConnectionFactory;
    }
}
