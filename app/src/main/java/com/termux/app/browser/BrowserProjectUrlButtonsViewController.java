package com.termux.app.browser;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.sessiondefinition.HttpSessionDefinitionDocumentFetcher;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.app.sessiondefinition.SessionDefinitionParser;
import com.termux.shared.logger.Logger;

import java.util.Collections;
import java.util.List;

public final class BrowserProjectUrlButtonsViewController {

    private static final String LOG_TAG = "BrowserProjectUrlButtonsViewController";

    private final TermuxActivity mActivity;

    private final TermuxBrowserController mBrowserController;

    private final LinearLayout mButtonsContainer;

    private final SessionDefinitionLoader mLoader;

    private final SessionDefinitionEntryMatcher mMatcher = new SessionDefinitionEntryMatcher();

    private List<SessionDefinitionEntry> mCachedEntries = Collections.emptyList();

    private String mPendingSessionName;

    public BrowserProjectUrlButtonsViewController(@NonNull TermuxActivity activity,
                                                  @NonNull TermuxBrowserController browserController) {
        this(activity, browserController,
            new SessionDefinitionLoader(new HttpSessionDefinitionDocumentFetcher(), new SessionDefinitionParser()));
    }

    public BrowserProjectUrlButtonsViewController(@NonNull TermuxActivity activity,
                                                  @NonNull TermuxBrowserController browserController,
                                                  @NonNull SessionDefinitionLoader loader) {
        this.mActivity = activity;
        this.mBrowserController = browserController;
        this.mButtonsContainer = activity.findViewById(R.id.browser_project_url_buttons);
        this.mLoader = loader;
    }

    public void showProjectUrlsForSession(@Nullable String sessionName) {
        mPendingSessionName = sessionName;

        if (sessionName == null || sessionName.isEmpty()) {
            renderButtons(Collections.emptyList());
            return;
        }

        if (!mCachedEntries.isEmpty()) {
            renderButtonsForSession(sessionName);
            return;
        }

        loadEntriesThenRender(sessionName);
    }

    private void loadEntriesThenRender(@NonNull String sessionName) {
        String baseUrl = mActivity.getPreferences().getSessionDefinitionUrl().trim();
        if (baseUrl.isEmpty()) {
            renderButtons(Collections.emptyList());
            return;
        }

        new Thread(() -> {
            try {
                List<SessionDefinitionEntry> entries = mLoader.load(baseUrl);
                mActivity.runOnUiThread(() -> {
                    mCachedEntries = entries;
                    if (isStaleRequest(sessionName)) return;
                    renderButtonsForSession(sessionName);
                });
            } catch (Exception exception) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load project URLs from " + baseUrl, exception);
                mActivity.runOnUiThread(() -> {
                    if (isStaleRequest(sessionName)) return;
                    renderButtons(Collections.emptyList());
                });
            }
        }).start();
    }

    private boolean isStaleRequest(@NonNull String sessionName) {
        return !sessionName.equals(mPendingSessionName);
    }

    private void renderButtonsForSession(@NonNull String sessionName) {
        SessionDefinitionEntry entry = mMatcher.findEntryForSessionName(mCachedEntries, sessionName);
        renderButtons(entry == null ? Collections.emptyList() : entry.getUrls());
    }

    private void renderButtons(@NonNull List<String> urls) {
        mButtonsContainer.removeAllViews();
        for (String url : urls) {
            mButtonsContainer.addView(createButton(url));
        }
    }

    private MaterialButton createButton(@NonNull String url) {
        MaterialButton button = new MaterialButton(mActivity, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setText(url);
        button.setAllCaps(false);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setMaxLines(1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBrowserController.openUrlInNewTab(url);
            }
        });
        return button;
    }

    @Nullable
    public String resolveProjectName(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty() || mCachedEntries.isEmpty()) {
            return null;
        }
        return mMatcher.findGroupLabelForSessionName(mCachedEntries, sessionName);
    }

    public void clearCachedEntries() {
        mCachedEntries = Collections.emptyList();
    }
}
