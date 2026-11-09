package com.termux.app.voicepatrol;

public final class LiveSessionHttpResponse {

    private final int mStatusCode;
    private final String mBody;

    public LiveSessionHttpResponse(int statusCode, String body) {
        mStatusCode = statusCode;
        mBody = body;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public String getBody() {
        return mBody;
    }
}
