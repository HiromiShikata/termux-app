package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsReportDelivery {

    public static final DiagnosticsReportDelivery NONE =
        new DiagnosticsReportDelivery(false, "", 0, 0L, false, false);

    private final boolean mAttempted;

    @NonNull
    private final String mSessionName;

    private final int mPastedCharacters;

    private final long mPasteMillis;

    private final boolean mEnterAcceptedForDelivery;

    private final boolean mInputReachedTheProgramAfterThePaste;

    private DiagnosticsReportDelivery(boolean attempted, @NonNull String sessionName, int pastedCharacters,
                                      long pasteMillis, boolean enterAcceptedForDelivery,
                                      boolean inputReachedTheProgramAfterThePaste) {
        mAttempted = attempted;
        mSessionName = sessionName;
        mPastedCharacters = pastedCharacters;
        mPasteMillis = pasteMillis;
        mEnterAcceptedForDelivery = enterAcceptedForDelivery;
        mInputReachedTheProgramAfterThePaste = inputReachedTheProgramAfterThePaste;
    }

    @NonNull
    public static DiagnosticsReportDelivery of(@NonNull String sessionName, int pastedCharacters,
                                               long pasteMillis, long bytesAcceptedBeforeTheEnter,
                                               long bytesAcceptedAfterTheEnter,
                                               boolean inputReachedTheProgramAfterThePaste) {
        return new DiagnosticsReportDelivery(true, sessionName, pastedCharacters, pasteMillis,
            bytesAcceptedAfterTheEnter > bytesAcceptedBeforeTheEnter, inputReachedTheProgramAfterThePaste);
    }

    public boolean wasAttempted() {
        return mAttempted;
    }

    @NonNull
    public String getSessionName() {
        return mSessionName;
    }

    public int getPastedCharacters() {
        return mPastedCharacters;
    }

    public long getPasteMillis() {
        return mPasteMillis;
    }

    public boolean wasEnterAcceptedForDelivery() {
        return mEnterAcceptedForDelivery;
    }

    public boolean didInputReachTheProgramAfterThePaste() {
        return mInputReachedTheProgramAfterThePaste;
    }
}
