package com.termux.app;

import androidx.annotation.NonNull;

public final class TermuxSessionCreationBatch {

    public interface WorkSharedByCreatedSessions {
        void run();
    }

    public void begin() {
    }

    public void runOrDefer(@NonNull WorkSharedByCreatedSessions work) {
        work.run();
    }

    public void end(@NonNull WorkSharedByCreatedSessions work) {
    }
}
