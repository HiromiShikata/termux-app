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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

final class WebRtcLiveVoiceConnection implements LiveVoiceConnection {

    private static final String LOG_TAG = "TermuxVoicePatrol";
    private static final String EVENT_CHANNEL_LABEL = "oai-events";
    private static final String MICROPHONE_TRACK_ID = "termux-voice-patrol-microphone";
    private static final String MEDIA_STREAM_ID = "termux-voice-patrol-stream";
    private static final String ICE_FAILED_REASON = "ice_failed";
    private static final String OFFER_FAILED_REASON = "offer_failed";
    private static final String LOCAL_DESCRIPTION_FAILED_REASON = "local_description_failed";
    private static final String EVENT_CHANNEL_CLOSED_REASON = "event_channel_closed";

    private final PeerConnectionFactory mFactory;
    private final ExecutorService mHttpExecutor;
    private final LiveSessionOfferExchange mOfferExchange;
    private final String mEndpointUrl;
    private final LiveVoiceConnectionListener mListener;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final LiveVoiceHandshake mHandshake = new LiveVoiceHandshake();
    private final Runnable mGatheringTimeout = () -> applyStep(mHandshake.onGatheringTimedOut());

    private PeerConnection mPeerConnection;
    private DataChannel mEventChannel;
    private AudioSource mAudioSource;
    private AudioTrack mMicrophoneTrack;
    private String mLocalOfferSdp = "";
    private boolean mConnected;
    private boolean mClosed;

    WebRtcLiveVoiceConnection(PeerConnectionFactory factory,
                              ExecutorService httpExecutor,
                              LiveSessionOfferExchange offerExchange,
                              String endpointUrl,
                              LiveVoiceConnectionListener listener) {
        mFactory = factory;
        mHttpExecutor = httpExecutor;
        mOfferExchange = offerExchange;
        mEndpointUrl = endpointUrl;
        mListener = listener;
    }

    void start() {
        mAudioSource = mFactory.createAudioSource(new MediaConstraints());
        mMicrophoneTrack = mFactory.createAudioTrack(MICROPHONE_TRACK_ID, mAudioSource);
        PeerConnection.RTCConfiguration configuration =
            new PeerConnection.RTCConfiguration(new ArrayList<PeerConnection.IceServer>());
        mPeerConnection = mFactory.createPeerConnection(configuration, new PeerObserver());
        if (mPeerConnection == null) {
            applyStep(mHandshake.onNegotiationFailed("peer_connection_unavailable"));
            return;
        }
        mMainHandler.postDelayed(mGatheringTimeout, LiveVoiceHandshake.GATHERING_TIMEOUT_MILLIS);
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
        mMainHandler.removeCallbacks(mGatheringTimeout);
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

    private void applyStep(HandshakeStep step) {
        if (mClosed) {
            return;
        }
        switch (step.getKind()) {
            case SEND_OFFER:
                mMainHandler.removeCallbacks(mGatheringTimeout);
                sendOffer(mLocalOfferSdp);
                break;
            case APPLY_ANSWER:
                applyAnswer(step.getSdp());
                break;
            case CONNECTED:
                mConnected = true;
                break;
            case FAIL:
                failWith(step.getReason());
                break;
            case DISCONNECTED:
                mListener.onDisconnected(step.getReason());
                close();
                break;
            default:
                break;
        }
    }

    private void failWith(String reason) {
        Log.w(LOG_TAG, "the live voice connection failed: " + reason);
        if (mConnected) {
            mListener.onDisconnected(reason);
        } else {
            mListener.onConnectFailed(reason);
        }
        close();
    }

    private void sendOffer(String offerSdp) {
        mHttpExecutor.execute(() -> {
            LiveSessionAnswer answer = mOfferExchange.exchange(mEndpointUrl, offerSdp);
            postToMain(() -> applyStep(mHandshake.onAnswerReceived(answer)));
        });
    }

    private void applyAnswer(String answerSdp) {
        mPeerConnection.setRemoteDescription(new SetObserver(
            () -> applyStep(mHandshake.onRemoteDescriptionApplied()),
            reason -> applyStep(mHandshake.onRemoteDescriptionFailed())),
            new SessionDescription(SessionDescription.Type.ANSWER, answerSdp));
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
            postToMain(() -> {
                mLocalOfferSdp = offer.description;
                mPeerConnection.setLocalDescription(new SetObserver(
                    () -> applyStep(mHandshake.onLocalDescriptionSet()),
                    reason -> applyStep(mHandshake.onNegotiationFailed(LOCAL_DESCRIPTION_FAILED_REASON))),
                    offer);
            });
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String reason) {
            postToMain(() -> applyStep(mHandshake.onNegotiationFailed(OFFER_FAILED_REASON)));
        }

        @Override
        public void onSetFailure(String reason) {
            postToMain(() -> applyStep(mHandshake.onNegotiationFailed(OFFER_FAILED_REASON)));
        }
    }

    private static final class SetObserver implements SdpObserver {
        private final Runnable mOnSuccess;
        private final FailureHandler mOnFailure;

        interface FailureHandler {
            void onFailure(String reason);
        }

        SetObserver(Runnable onSuccess, FailureHandler onFailure) {
            mOnSuccess = onSuccess;
            mOnFailure = onFailure;
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
            Log.w(LOG_TAG, "setting a session description failed: " + reason);
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
                postToMain(() -> applyStep(mHandshake.onConnectionLost(ICE_FAILED_REASON)));
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
            if (state == PeerConnection.IceGatheringState.COMPLETE) {
                postToMain(() -> applyStep(mHandshake.onIceGatheringComplete()));
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
        private boolean mOpenSeen;

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

        private void onEventChannelState(DataChannel.State state) {
            if (state == DataChannel.State.OPEN && !mOpenSeen) {
                mOpenSeen = true;
                applyStep(mHandshake.onDataChannelOpened());
            } else if (state == DataChannel.State.CLOSED) {
                applyStep(mHandshake.onConnectionLost(EVENT_CHANNEL_CLOSED_REASON));
            }
        }
    }
}
