package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class FinishedSessionPendingInput {

    private static final int MAX_PENDING_INPUT_LENGTH = 4096;

    @NonNull
    private final Map<String, StringBuilder> pendingInputBySessionHandle = new HashMap<>();

    public void recordCodePoint(@NonNull String sessionHandle, int codePoint) {
        if (codePoint < 0) return;
        StringBuilder buffer = pendingInputBySessionHandle.get(sessionHandle);
        if (buffer == null) {
            buffer = new StringBuilder();
            pendingInputBySessionHandle.put(sessionHandle, buffer);
        }
        if (buffer.length() >= MAX_PENDING_INPUT_LENGTH) return;
        buffer.appendCodePoint(codePoint);
    }

    @NonNull
    public String consume(@NonNull String sessionHandle) {
        StringBuilder buffer = pendingInputBySessionHandle.remove(sessionHandle);
        if (buffer == null) return "";
        return buffer.toString();
    }

    public void discard(@NonNull String sessionHandle) {
        pendingInputBySessionHandle.remove(sessionHandle);
    }
}
