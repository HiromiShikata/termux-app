package com.termux.app;

import androidx.annotation.NonNull;

public final class TermuxSessionCreationBatch {

    public interface WorkSharedByCreatedSessions {
        void run();
    }

    private int mOpenRestores;

    private boolean mWorkIsPending;

    public void begin() {
        mOpenRestores++;
    }

    public void runOrDefer(@NonNull WorkSharedByCreatedSessions work) {
        if (mOpenRestores > 0) {
            mWorkIsPending = true;
            return;
        }
        work.run();
    }

    public void end(@NonNull WorkSharedByCreatedSessions work) {
        if (mOpenRestores == 0) return;
        mOpenRestores--;
        if (mOpenRestores > 0) return;
        if (!mWorkIsPending) return;
        mWorkIsPending = false;
        work.run();
    }
}
