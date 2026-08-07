package com.termux.app.link;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.app.browser.OpenTagBrowserController;

public final class OpenTagUrlNativeAppOpener implements OpenTagBrowserController.UrlOpener {

    private final Context mContext;

    private final OpenTagBrowserController.UrlOpener mInAppBrowserOpener;

    public OpenTagUrlNativeAppOpener(@NonNull Context context,
                                     @NonNull OpenTagBrowserController.UrlOpener inAppBrowserOpener) {
        this.mContext = context;
        this.mInAppBrowserOpener = inAppBrowserOpener;
    }

    @Override
    public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url) {
        NativeAppLink.openInNativeAppOrElse(mContext, url,
            () -> mInAppBrowserOpener.openUrlInTabForSession(sessionHandle, url));
    }
}
