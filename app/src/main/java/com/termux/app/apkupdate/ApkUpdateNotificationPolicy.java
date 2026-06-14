package com.termux.app.apkupdate;

public final class ApkUpdateNotificationPolicy {

    public boolean shouldNotifyUpToDate(boolean userInitiated) {
        return userInitiated;
    }

    public boolean shouldNotifyCheckFailed(boolean userInitiated) {
        return userInitiated;
    }
}
