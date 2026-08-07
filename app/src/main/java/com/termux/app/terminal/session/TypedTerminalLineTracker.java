package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public final class TypedTerminalLineTracker {

    @NonNull
    private final Set<String> typedSinceLastLineEndBySessionHandle = new HashSet<>();

    public static boolean isPrintableCodePoint(int codePoint) {
        return codePoint >= 0x20 && codePoint != 0x7F;
    }

    public static boolean isLineEndCodePoint(int codePoint) {
        return codePoint == '\r' || codePoint == '\n';
    }

    public void recordCodePoint(@NonNull String sessionHandle, int codePoint) {
        if (!isPrintableCodePoint(codePoint)) return;
        typedSinceLastLineEndBySessionHandle.add(sessionHandle);
    }

    public boolean consumeTypedLine(@NonNull String sessionHandle) {
        return typedSinceLastLineEndBySessionHandle.remove(sessionHandle);
    }
}
