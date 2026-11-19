package com.termux.app.apkupdate;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DownloadFlagRelease {

    public static void releaseAfter(AtomicBoolean downloadInProgress, Runnable persist) {
        try {
            persist.run();
        } finally {
            downloadInProgress.set(false);
        }
    }
}
