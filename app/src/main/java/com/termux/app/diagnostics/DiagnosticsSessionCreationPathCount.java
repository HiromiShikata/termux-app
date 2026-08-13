package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsSessionCreationPathCount {

    @NonNull
    private final SessionCreationPath mPath;

    private final int mCreationCount;

    public DiagnosticsSessionCreationPathCount(@NonNull SessionCreationPath path, int creationCount) {
        mPath = path;
        mCreationCount = creationCount;
    }

    @NonNull
    public SessionCreationPath getPath() {
        return mPath;
    }

    public int getCreationCount() {
        return mCreationCount;
    }
}
