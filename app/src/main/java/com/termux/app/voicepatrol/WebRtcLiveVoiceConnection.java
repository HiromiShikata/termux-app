package com.termux.app.voicepatrol;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

final class WebRtcLiveVoiceConnection implements LiveVoiceConnection {

    private static final String LOG_TAG = "TermuxVoicePatrol";
    private static final String STUN_SERVER_URL = "stun:stun.l.google.com:19302";
    private static final String EVENT_CHANNEL_LABEL = "oai-events";
    private static final String MICROPHONE_TRACK_ID = "termux-voice-patrol-microphone";
    private static final String MEDIA_STREAM_ID = "termux-voice-patrol-stream";
    private static final String SDP_CONTENT_TYPE = "application/sdp";
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final String ICE_FAILED_REASON = "ice_failed";

    private final PeerConnectionFactory mFactory;
    private final ExecutorService mHttpExecutor;
    private final String mEndpointUrl;
    private final LiveVoiceConnectionListener mListener;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private PeerConnection mPeerConnection;
    private DataChannel mEventChannel;
    private AudioSource mAudioSource;
    private AudioTrack mMicrophoneTrack;
    private boolean mLocalDescriptionReady;
    private boolean mIceGatheringComplete;
    private boolean mOfferSent;
    private boolean mEventChannelOpen;
    private boolean mClosed;

    WebRtcLiveVoiceConnection(PeerConnectionFactory factory,
                              ExecutorService httpExecutor,
                              String endpointUrl,
                              LiveVoiceConnectionListener listener) {
        mFactory = factory;
        mHttpExecutor = httpExecutor;
        mEndpointUrl = endpointUrl;
        mListener = listener;
    }

    void start() {
        mAudioSource = mFactory.createAudioSource(new MediaConstraints());
        mMicrophoneTrack = mFactory.createAudioTrack(MICROPHONE_TRACK_ID, mAudioSource);
        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(
            Collections.singletonList(PeerConnection.IceServer.builder(STUN_SERVER_URL).createIceServer()));
        mPeerConnection = mFactory.createPeerConnection(configuration, new PeerObserver());
        if (mPeerConnection == null) {
            failBeforeConnected("peer_connection_unavailable");
            return;
        }
        mPeerConnection.addTrack(mMicrophoneTrack, Collections.singletonList(MEDIA_STREAM_ID));
        mEventChannel = mPeerConnection.createDataChannel(EVENT_CHANNEL_LABEL, new DataChannel.Init());
        mEventChannel.registerObserver(new EventChannelObserver());
        mPeerConnection.createOffer(new OfferObserver(), new MediaConstraints());
    }

    @Override
    public void sendEvent(String eventJson) {
        if (mEventChannel == null || mEventChannel.state() != DataChannel.State.OPEN) {
            Log.w(LOG_TAG, "dropping a live event because the event channel is not open");
            return;
        }
        byte[] bytes = eventJson.getBytes(StandardCharsets.UTF_8);
        mEventChannel.send(new DataChannel.Buffer(ByteBuffer.wrap(bytes), false));
    }

    @Override
    public void setMicrophoneEnabled(boolean enabled) {
        if (mMicrophoneTrack != null) {
            mMicrophoneTrack.setEnabled(enabled);
        }
    }

    @Override
    public void close() {
        if (mClosed) {
            return;
        }
        mClosed = true;
        if (mEventChannel != null) {
            mEventChannel.close();
            mEventChannel.dispose();
        }
        if (mPeerConnection != null) {
            mPeerConnection.close();
            mPeerConnection.dispose();
        }
        if (mMicrophoneTrack != null) {
            mMicrophoneTrack.dispose();
        }
        if (mAudioSource != null) {
            mAudioSource.dispose();
        }
    }

    private void onLocalOfferReady() {
        if (mOfferSent || !mLocalDescriptionReady || !mIceGatheringComplete || mClosed) {
            return;
        }
        mOfferSent = true;
        SessionDescription localDescription = mPeerConnection.getLocalDescription();
        String offerSdp = localDescription.description;
        mHttpExecutor.execute(() -> postOffer(offerSdp));
    }

    private void postOffer(String offerSdp) {
        LiveSessionAnswer answer;
        try {
            answer = exchangeOffer(offerSdp);
        } catch (IOException networkFailure) {
            Log.w(LOG_TAG, "the live session request failed: " + networkFailure.getMessage(), networkFailure);
            postToMain(() -> failBeforeConnected("network_error"));
            return;
        }
        postToMain(() -> onAnswer(answer));
    }

    private LiveSessionAnswer exchangeOffer(String offerSdp) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(mEndpointUrl).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty("Content-Type", SDP_CONTENT_TYPE);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(offerSdp.getBytes(StandardCharsets.UTF_8));
            }
            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = stream == null ? "" : readFully(stream);
            return LiveSessionAnswer.fromHttpResponse(statusCode, body);
        } finally {
            connection.disconnect();
        }
    }

    private static String readFully(InputStream stream) throws IOException {
        try (InputStream input = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void onAnswer(LiveSessionAnswer answer) {
        if (mClosed) {
            return;
        }
        if (!answer.isAccepted()) {
            Log.w(LOG_TAG, "the live session was refused: " + answer.getFailureReason());
            failBeforeConnected(answer.getFailureReason());
            return;
        }
        mPeerConnection.setRemoteDescription(new SetObserver(
            () -> {
            },
            reason -> failBeforeConnected("remote_description_failed"),
            "remote description"),
            new SessionDescription(SessionDescription.Type.ANSWER, answer.getSdp()));
    }

    private void failBeforeConnected(String reason) {
        if (mClosed) {
            return;
        }
        if (mEventChannelOpen) {
            mListener.onDisconnected(reason);
        } else {
            mListener.onConnectFailed(reason);
        }
        close();
    }

    private void postToMain(Runnable action) {
        mMainHandler.post(() -> {
            if (!mClosed) {
                action.run();
            }
        });
    }

    private final class OfferObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription offer) {
            postToMain(() -> mPeerConnection.setLocalDescription(
                new SetObserver(
                    () -> {
                        mLocalDescriptionReady = true;
                        onLocalOfferReady();
                    },
                    reason -> failBeforeConnected("local_description_failed"),
                    "local description"),
                offer));
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String reason) {
            postToMain(() -> failBeforeConnected("offer_failed"));
        }

        @Override
        public void onSetFailure(String reason) {
            postToMain(() -> failBeforeConnected("offer_failed"));
        }
    }

    private static final class SetObserver implements SdpObserver {
        private final Runnable mOnSuccess;
        private final FailureReasonHandler mOnFailure;
        private final String mDescriptionName;

        interface FailureReasonHandler {
            void onFailure(String reason);
        }

        SetObserver(Runnable onSuccess, FailureReasonHandler onFailure, String descriptionName) {
            mOnSuccess = onSuccess;
            mOnFailure = onFailure;
            mDescriptionName = descriptionName;
        }

        @Override
        public void onCreateSuccess(SessionDescription description) {
        }

        @Override
        public void onSetSuccess() {
            mOnSuccess.run();
        }

        @Override
        public void onCreateFailure(String reason) {
            mOnFailure.onFailure(reason);
        }

        @Override
        public void onSetFailure(String reason) {
            Log.w(LOG_TAG, "setting the " + mDescriptionName + " failed: " + reason);
            mOnFailure.onFailure(reason);
        }
    }

    private final class PeerObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState state) {
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
            if (state == PeerConnection.IceConnectionState.FAILED) {
                postToMain(() -> failBeforeConnected(ICE_FAILED_REASON));
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
            if (state == PeerConnection.IceGatheringState.COMPLETE) {
                postToMain(() -> {
                    mIceGatheringComplete = true;
                    onLocalOfferReady();
                });
            }
        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        }

        @Override
        public void onAddStream(MediaStream stream) {
        }

        @Override
        public void onRemoveStream(MediaStream stream) {
        }

        @Override
        public void onDataChannel(DataChannel channel) {
        }

        @Override
        public void onRenegotiationNeeded() {
        }

        @Override
        public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {
        }
    }

    private final class EventChannelObserver implements DataChannel.Observer {
        @Override
        public void onBufferedAmountChange(long previousAmount) {
        }

        @Override
        public void onStateChange() {
            DataChannel.State state = mEventChannel.state();
            postToMain(() -> onEventChannelState(state));
        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            if (buffer.binary) {
                Log.w(LOG_TAG, "ignoring a binary live event");
                return;
            }
            byte[] bytes = new byte[buffer.data.remaining()];
            buffer.data.get(bytes);
            String eventJson = new String(bytes, StandardCharsets.UTF_8);
            postToMain(() -> mListener.onEventReceived(eventJson));
        }
    }

    private void onEventChannelState(DataChannel.State state) {
        if (state == DataChannel.State.OPEN) {
            mEventChannelOpen = true;
        } else if (state == DataChannel.State.CLOSED && mEventChannelOpen) {
            mListener.onDisconnected("event_channel_closed");
            close();
        }
    }
}
