package com.termux.app.apkupdate;

public final class ApkUpdateNotificationPolicy {

    public boolean shouldNotifyUpToDate(boolean userInitiated) {
        return userInitiated;
    }

    /**
     * Whether a failed check should be surfaced to the user. A user-initiated check always reports
     * its failure. A rate-limit failure is also surfaced on a non-user-initiated (auto) check,
     * because that is the exact condition under which the app would otherwise silently show nothing
     * while a newer release exists; a plain network error on an auto check stays quiet to avoid noise.
     */
    public boolean shouldNotifyCheckFailed(boolean userInitiated, boolean rateLimited) {
        return userInitiated || rateLimited;
    }
}
