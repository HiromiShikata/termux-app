package com.termux.app.voicepatrol;

public final class LiveServerEvent {

    private final LiveServerEventKind mKind;
    private final String mCallId;
    private final String mName;
    private final String mArguments;

    private LiveServerEvent(LiveServerEventKind kind, String callId, String name, String arguments) {
        mKind = kind;
        mCallId = callId;
        mName = name;
        mArguments = arguments;
    }

    public static LiveServerEvent ofKind(LiveServerEventKind kind) {
        return new LiveServerEvent(kind, "", "", "");
    }

    public static LiveServerEvent functionCall(String callId, String name, String arguments) {
        return new LiveServerEvent(LiveServerEventKind.FUNCTION_CALL, callId, name, arguments);
    }

    public LiveServerEventKind getKind() {
        return mKind;
    }

    public String getCallId() {
        return mCallId;
    }

    public String getName() {
        return mName;
    }

    public String getArguments() {
        return mArguments;
    }
}
