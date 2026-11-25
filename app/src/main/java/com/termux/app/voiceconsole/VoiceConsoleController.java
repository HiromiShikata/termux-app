package com.termux.app.voiceconsole;

import android.util.Log;

public final class VoiceConsoleController {

    private static final String LOG_TAG = "TermuxVoiceConsole";

    public interface FailureReporter {
        void onFailed(String reason);
    }

    private enum State {
        STOPPED,
        CONNECTING,
        CONNECTED
    }

    private final VoiceConsoleEndpoint mEndpoint;
    private final LiveVoiceConnectionFactory mFactory;
    private final FailureReporter mFailureReporter;
    private State mState = State.STOPPED;
    private LiveVoiceConnection mConnection;

    public VoiceConsoleController(VoiceConsoleEndpoint endpoint, LiveVoiceConnectionFactory factory,
                                  FailureReporter failureReporter) {
        mEndpoint = endpoint;
        mFactory = factory;
        mFailureReporter = failureReporter;
    }

    public boolean isRunning() {
        return mState != State.STOPPED;
    }

    public boolean isConnected() {
        return mState == State.CONNECTED;
    }

    public void start() {
        if (mState != State.STOPPED) {
            return;
        }
        mState = State.CONNECTING;
        LiveVoiceConnection connection = mFactory.open(mEndpoint, new Listener());
        if (mState == State.STOPPED) {
            closeSession(connection);
            return;
        }
        mConnection = connection;
    }

    public void stop() {
        if (mState == State.STOPPED) {
            return;
        }
        LiveVoiceConnection connection = mConnection;
        mState = State.STOPPED;
        mConnection = null;
        if (connection != null) {
            closeSession(connection);
        }
    }

    private static void closeSession(LiveVoiceConnection connection) {
        connection.sendEvent(VoiceConsoleEvent.SESSION_CLOSE_EVENT_JSON);
        connection.close();
    }

    private void endWithFailure(String reason) {
        if (mState == State.STOPPED) {
            return;
        }
        Log.w(LOG_TAG, "the voice console session ended: " + reason);
        LiveVoiceConnection connection = mConnection;
        mState = State.STOPPED;
        mConnection = null;
        if (connection != null) {
            connection.close();
        }
        mFailureReporter.onFailed(reason);
    }

    private final class Listener implements LiveVoiceConnectionListener {

        @Override
        public void onConnected() {
            if (mState == State.CONNECTING) {
                mState = State.CONNECTED;
            }
        }

        @Override
        public void onEventReceived(String eventJson) {
            String endingReason = VoiceConsoleEvent.endingReasonOf(eventJson);
            if (endingReason != null) {
                endWithFailure(endingReason);
            }
        }

        @Override
        public void onConnectFailed(String reason) {
            endWithFailure(reason);
        }

        @Override
        public void onDisconnected(String reason) {
            endWithFailure(reason);
        }
    }
}
