package com.termux.app.apkupdate;

import android.content.Context;

public final class ApkUpdateNotifyDecider implements ApkUpdateManager.CheckListener {

    public interface NotifyAction {
        void dispatch(Context context, ApkUpdateAvailability availability);
    }

    private final Context context;
    private final NotifyAction notifyAction;
    private final Runnable onComplete;

    public ApkUpdateNotifyDecider(Context context, Runnable onComplete) {
        this(context, ApkUpdateNotifier::notify, onComplete);
    }

    ApkUpdateNotifyDecider(Context context, NotifyAction notifyAction, Runnable onComplete) {
        this.context = context;
        this.notifyAction = notifyAction;
        this.onComplete = onComplete;
    }

    @Override
    public void onUpdateAvailable(ApkUpdateAvailability availability) {
        notifyAction.dispatch(context, availability);
        onComplete.run();
    }

    @Override
    public void onUpToDate(String latestVersionName) {
        onComplete.run();
    }

    @Override
    public void onCheckFailed(String message, boolean rateLimited) {
        onComplete.run();
    }
}
