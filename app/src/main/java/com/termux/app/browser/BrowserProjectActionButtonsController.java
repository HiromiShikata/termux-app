package com.termux.app.browser;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BrowserProjectActionButtonsController {

    public interface ProjectActionUrlOpener {
        void openProjectActionUrl(@NonNull String url, @NonNull BrowserViewMode viewMode);
    }

    private final View mOverviewButton;

    private final View mTdpmConsoleButton;

    private final View mNewIssueButton;

    private final ProjectActionUrlOpener mUrlOpener;

    private BrowserProjectActionUrls mActionUrls = BrowserProjectActionUrls.EMPTY;

    public BrowserProjectActionButtonsController(@NonNull View overviewButton,
                                                 @NonNull View tdpmConsoleButton,
                                                 @NonNull View newIssueButton,
                                                 @NonNull ProjectActionUrlOpener urlOpener) {
        this.mOverviewButton = overviewButton;
        this.mTdpmConsoleButton = tdpmConsoleButton;
        this.mNewIssueButton = newIssueButton;
        this.mUrlOpener = urlOpener;
        mOverviewButton.setOnClickListener(view -> openAction(mActionUrls.getOverviewUrl(), BrowserViewMode.DESKTOP));
        mTdpmConsoleButton.setOnClickListener(view -> openAction(mActionUrls.getTdpmConsoleUrl(), BrowserViewMode.MOBILE));
        mNewIssueButton.setOnClickListener(view -> openAction(mActionUrls.getNewIssueUrl(), BrowserViewMode.DESKTOP));
    }

    public void setActionUrls(@NonNull BrowserProjectActionUrls actionUrls) {
        mActionUrls = actionUrls;
        applyButtonVisibility(mOverviewButton, actionUrls.getOverviewUrl());
        applyButtonVisibility(mTdpmConsoleButton, actionUrls.getTdpmConsoleUrl());
        applyButtonVisibility(mNewIssueButton, actionUrls.getNewIssueUrl());
    }

    private void openAction(@Nullable String url, @NonNull BrowserViewMode viewMode) {
        if (url == null || url.isEmpty()) {
            return;
        }
        mUrlOpener.openProjectActionUrl(url, viewMode);
    }

    private static void applyButtonVisibility(@NonNull View button, @Nullable String url) {
        button.setVisibility(url == null || url.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
