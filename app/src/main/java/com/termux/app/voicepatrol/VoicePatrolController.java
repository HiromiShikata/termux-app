package com.termux.app.voicepatrol;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VoicePatrolController implements LiveVoiceConnectionListener {

    private static final String CONNECTION_FAILED_MESSAGE_JA = "音声の接続を始められませんでした。";
    private static final String SPEECH_FAILED_MESSAGE_JA = "読み上げに失敗しました。";
    private static final String REPLY_NOT_DELIVERED_MESSAGE_JA = "返信を送れませんでした。";
    private static final String CONNECTION_LOST_MESSAGE_JA = "音声の接続が切れました。";
    private static final String CONTEXT_CONTENT_HEADER = "Session name: %s\nOwner call body (already read aloud to the owner; do not read it aloud):\n%s";
    private static final String CONTEXT_EVENT_ID_PREFIX = "patrol-context";
    private static final String EVENT_ID_PREFIX = "patrol-event-";

    private final String mEndpointUrl;
    private final VoicePatrolCallSource mCallSource;
    private final VoicePatrolSpeech mSpeech;
    private final VoicePatrolSessionActions mSessions;
    private final LiveVoiceConnectionFactory mConnections;
    private final VoicePatrolLog mLog;

    private final Set<String> mExcludedSessionNames = new HashSet<>();

    private VoicePatrolPhase mPhase = VoicePatrolPhase.STOPPED;
    private VoicePatrolCall mCurrentCall;
    private boolean mReadingForContext;
    private boolean mSessionStarted;
    private boolean mClosing;
    private LiveVoiceConnection mConnection;
    private Set<String> mWaitingSnapshotNames = new HashSet<>();
    private int mEventCounter;

    public VoicePatrolController(String endpointUrl,
                                 VoicePatrolCallSource callSource,
                                 VoicePatrolSpeech speech,
                                 VoicePatrolSessionActions sessions,
                                 LiveVoiceConnectionFactory connections,
                                 VoicePatrolLog log) {
        mEndpointUrl = endpointUrl;
        mCallSource = callSource;
        mSpeech = speech;
        mSessions = sessions;
        mConnections = connections;
        mLog = log;
    }

    public VoicePatrolPhase getPhase() {
        return mPhase;
    }

    public void start() {
        if (mPhase != VoicePatrolPhase.STOPPED) {
            return;
        }
        mPhase = VoicePatrolPhase.WAITING_FOR_CALL;
        pickNext();
    }

    public void stop() {
        mPhase = VoicePatrolPhase.STOPPED;
        mCurrentCall = null;
        mExcludedSessionNames.clear();
        mSpeech.stop();
        closeConnection();
    }

    public void onCallingSessionsChanged() {
        if (mPhase != VoicePatrolPhase.WAITING_FOR_CALL) {
            return;
        }
        List<VoicePatrolCall> calls = mCallSource.callingSessionsInBellOrder();
        Set<String> names = namesOf(calls);
        boolean hasNewName = false;
        for (String name : names) {
            if (!mWaitingSnapshotNames.contains(name)) {
                hasNewName = true;
            }
        }
        if (hasNewName) {
            pickNext();
        } else {
            mWaitingSnapshotNames = names;
        }
    }

    @Override
    public void onEventReceived(String eventJson) {
        LiveServerEvent event;
        try {
            event = LiveEvents.parseServerEvent(eventJson);
        } catch (IllegalArgumentException unreadable) {
            mLog.warn("Ignoring a live event that cannot be read: " + unreadable.getMessage());
            return;
        }
        switch (event.getKind()) {
            case SESSION_STARTED:
                onSessionStarted();
                break;
            case SESSION_CLOSED:
                onSessionClosed();
                break;
            case FUNCTION_CALL:
                handleFunctionCall(event);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectFailed(String reason) {
        mLog.warn("Live voice connection failed: " + reason);
        closeConnection();
        speakAndWait(CONNECTION_FAILED_MESSAGE_JA + VoicePatrolFailureMessages.japaneseFor(reason));
    }

    @Override
    public void onDisconnected(String reason) {
        mLog.warn("Live voice connection was lost: " + reason);
        closeConnection();
        if (mPhase != VoicePatrolPhase.WAITING_FOR_CALL && mPhase != VoicePatrolPhase.STOPPED) {
            enterWaiting(namesOf(mCallSource.callingSessionsInBellOrder()));
            speakWithoutFollowUp(CONNECTION_LOST_MESSAGE_JA);
        }
    }

    private void pickNext() {
        List<VoicePatrolCall> calls = mCallSource.callingSessionsInBellOrder();
        Set<String> names = namesOf(calls);
        mExcludedSessionNames.retainAll(names);
        for (VoicePatrolCall call : calls) {
            if (!mExcludedSessionNames.contains(call.getSessionName())) {
                beginRead(call, true);
                return;
            }
        }
        enterWaiting(names);
    }

    private void beginRead(VoicePatrolCall call, boolean appendContextAfterReading) {
        if (mClosing) {
            closeConnection();
        }
        mPhase = VoicePatrolPhase.READING_CALL;
        mCurrentCall = call;
        mReadingForContext = appendContextAfterReading;
        mSessions.showSession(call.getSessionName());
        setMicrophoneEnabled(false);
        mSpeech.speak(call.getBody(), new VoicePatrolSpeechOutcome() {
            @Override
            public void onSpoken() {
                onSpeechDone(call);
            }

            @Override
            public void onFailed(String reason) {
                onSpeechFailed(call, reason);
            }
        });
    }

    private void onSpeechFailed(VoicePatrolCall call, String reason) {
        if (mPhase != VoicePatrolPhase.READING_CALL || call != mCurrentCall) {
            return;
        }
        mLog.warn("Reading the owner call of " + call.getSessionName() + " failed: " + reason);
        enterWaiting(namesOf(mCallSource.callingSessionsInBellOrder()));
        speakWithoutFollowUp(SPEECH_FAILED_MESSAGE_JA + VoicePatrolFailureMessages.japaneseFor(reason));
    }

    private void onSpeechDone(VoicePatrolCall call) {
        if (mPhase != VoicePatrolPhase.READING_CALL || call != mCurrentCall) {
            return;
        }
        if (mConnection == null) {
            openConnection();
            return;
        }
        if (mSessionStarted) {
            if (mReadingForContext) {
                appendContext(call);
            }
            mPhase = VoicePatrolPhase.CONVERSING;
            setMicrophoneEnabled(true);
        } else {
            mPhase = VoicePatrolPhase.CONNECTING;
        }
    }

    private void openConnection() {
        mPhase = VoicePatrolPhase.CONNECTING;
        mSessionStarted = false;
        mClosing = false;
        mConnection = mConnections.open(mEndpointUrl, this);
    }

    private void onSessionStarted() {
        mSessionStarted = true;
        if (mPhase == VoicePatrolPhase.CONNECTING && mCurrentCall != null) {
            appendContext(mCurrentCall);
            mPhase = VoicePatrolPhase.CONVERSING;
            setMicrophoneEnabled(true);
        }
    }

    private void onSessionClosed() {
        boolean closedByServer = !mClosing;
        if (closedByServer) {
            mLog.warn("The live session was closed by the server");
        }
        closeConnection();
        if (mPhase != VoicePatrolPhase.WAITING_FOR_CALL && mPhase != VoicePatrolPhase.STOPPED) {
            enterWaiting(namesOf(mCallSource.callingSessionsInBellOrder()));
            if (closedByServer) {
                speakWithoutFollowUp(CONNECTION_LOST_MESSAGE_JA);
            }
        }
    }

    private void handleFunctionCall(LiveServerEvent event) {
        String output;
        boolean advanceToNextCall = false;
        boolean repeatCurrentCall = false;
        if (mCurrentCall == null) {
            mLog.warn("A tool call arrived with no calling session selected: " + event.getName());
            output = errorOutput("no_calling_session_selected");
        } else if (LiveToolCalls.SESSION_REPLY_SEND.equals(event.getName())) {
            LiveToolCalls.ReplyText reply = LiveToolCalls.replyTextOf(event.getArguments());
            if (!reply.isValid() || reply.getValue().isEmpty()) {
                mLog.warn("session_reply_send has no usable replyText: " + event.getArguments());
                output = errorOutput("invalid_arguments");
            } else {
                String sessionName = mCurrentCall.getSessionName();
                if (mSessions.sendReply(sessionName, reply.getValue())) {
                    mExcludedSessionNames.add(sessionName);
                    output = statusOutput("sent");
                    advanceToNextCall = true;
                } else {
                    mLog.warn("The reply to " + sessionName + " was not delivered to its terminal");
                    output = errorOutput("reply_not_delivered");
                    speakWithoutFollowUp(REPLY_NOT_DELIVERED_MESSAGE_JA);
                }
            }
        } else if (LiveToolCalls.SESSION_SKIP.equals(event.getName())) {
            mExcludedSessionNames.add(mCurrentCall.getSessionName());
            output = statusOutput("skipped");
            advanceToNextCall = true;
        } else if (LiveToolCalls.OWNER_CALL_REPEAT.equals(event.getName())) {
            output = statusOutput("repeating");
            repeatCurrentCall = true;
        } else {
            mLog.warn("Unknown tool call: " + event.getName());
            output = errorOutput("unknown_tool");
        }
        sendEvent(LiveEvents.functionCallOutput(nextEventId(), event.getCallId(), output));
        sendEvent(LiveEvents.responseCreate(nextEventId()));
        if (advanceToNextCall) {
            pickNext();
        } else if (repeatCurrentCall) {
            beginRead(mCurrentCall, false);
        }
    }

    private void enterWaiting(Set<String> names) {
        mPhase = VoicePatrolPhase.WAITING_FOR_CALL;
        mCurrentCall = null;
        mWaitingSnapshotNames = names;
        setMicrophoneEnabled(false);
        if (mConnection != null && mSessionStarted && !mClosing) {
            mClosing = true;
            sendEvent(LiveEvents.sessionClose(nextEventId()));
        } else if (mConnection != null && !mSessionStarted) {
            closeConnection();
        }
    }

    private void speakAndWait(String message) {
        mPhase = VoicePatrolPhase.WAITING_FOR_CALL;
        mCurrentCall = null;
        mWaitingSnapshotNames = namesOf(mCallSource.callingSessionsInBellOrder());
        speakWithoutFollowUp(message);
    }

    private void speakWithoutFollowUp(String message) {
        mSpeech.speak(message, new VoicePatrolSpeechOutcome() {
            @Override
            public void onSpoken() {
            }

            @Override
            public void onFailed(String reason) {
                mLog.warn("Spoken message could not be read aloud: " + reason);
            }
        });
    }

    private void appendContext(VoicePatrolCall call) {
        String content = String.format(CONTEXT_CONTENT_HEADER, call.getSessionName(), call.getBody());
        for (String event : LiveEvents.sessionThinkingAppends(
                CONTEXT_EVENT_ID_PREFIX + "-" + mEventCounter++, content)) {
            sendEvent(event);
        }
    }

    private void closeConnection() {
        LiveVoiceConnection connection = mConnection;
        mConnection = null;
        mSessionStarted = false;
        mClosing = false;
        if (connection != null) {
            connection.close();
        }
    }

    private void setMicrophoneEnabled(boolean enabled) {
        if (mConnection != null) {
            mConnection.setMicrophoneEnabled(enabled);
        }
    }

    private void sendEvent(String eventJson) {
        if (mConnection != null) {
            mConnection.sendEvent(eventJson);
        }
    }

    private String nextEventId() {
        return EVENT_ID_PREFIX + mEventCounter++;
    }

    private static Set<String> namesOf(List<VoicePatrolCall> calls) {
        Set<String> names = new HashSet<>();
        for (VoicePatrolCall call : calls) {
            names.add(call.getSessionName());
        }
        return names;
    }

    private static String statusOutput(String status) {
        return jsonObject("status", status);
    }

    private static String errorOutput(String error) {
        return jsonObject("error", error);
    }

    private static String jsonObject(String key, String value) {
        try {
            return new JSONObject().put(key, value).toString();
        } catch (JSONException impossibleForStringValues) {
            throw new IllegalStateException(impossibleForStringValues);
        }
    }
}
