package com.termux.app.link;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.app.browser.OpenTagBrowserController;

public final class OpenTagUrlGoogleAppOpener implements OpenTagBrowserController.UrlOpener {

    private final Context mContext;

    private final OpenTagBrowserController.UrlOpener mInAppBrowserOpener;

    public OpenTagUrlGoogleAppOpener(@NonNull Context context,
                                     @NonNull OpenTagBrowserController.UrlOpener inAppBrowserOpener) {
        this.mContext = context;
        this.mInAppBrowserOpener = inAppBrowserOpener;
    }

    @Override
    public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url) {
        GoogleAppLink.openInGoogleAppOrElse(mContext, url,
            () -> mInAppBrowserOpener.openUrlInTabForSession(sessionHandle, url));
    }
}
