package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.SessionListBottomSheetController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TermuxBrowserController implements BrowserTabSelectionListener {

    private static final String LOG_TAG = "TermuxBrowserController";

    private static final String BROWSER_PAGE_TEXT_FILE_NAME = "browser-page.txt";

    private static final float HEADER_SECONDARY_TEXT_SCALE = 0.85f;

    private static final int HEADER_SECONDARY_TEXT_ALPHA = 0xB3;

    private final TermuxActivity mActivity;

    private final BrowserTabManager mTabManager = new BrowserTabManager();

    private final BrowserSessionVisibilityState mSessionVisibilityState = new BrowserSessionVisibilityState();

    private final BrowserBookmarkSerializer mBookmarkSerializer = new BrowserBookmarkSerializer();

    private final WebView mWebView;

    private final View mBrowserContentContainer;

    private final View mBrowserTerminalDivider;

    private float mSplitDragStartRawY;

    private int mSplitDragStartBrowserHeight;

    private int mSplitDragTotalHeight;

    private Float mLastAppliedBrowserSplitRatio;

    private final TextView mPageTitleUrlHeaderView;

    private final SwipeRefreshLayout mSwipeRefreshLayout;

    private final ProgressBar mPageLoadProgressBar;

    private final View mWebViewCover;

    private final ListView mTabsListView;

    private CheckBox mDesktopModeToggle;

    private String mDefaultUserAgent;

    private BrowserTabsListViewController mTabsListViewController;

    private final BrowserProjectNameResolver mProjectNameResolver;

    private final View mProjectOverviewActionsView;

    private BrowserBulkOpenController mBulkOpenController;

    private String mCurrentSessionHandle;

    private String mCurrentSessionName;

    private BrowserRenderedFrame mRenderedFrame = BrowserRenderedFrame.BLANK;

    private boolean mBrowserVisible;

    private final Set<Long> mEnqueuedDownloadIds = new HashSet<>();

    private BroadcastReceiver mDownloadCompleteReceiver;

    private boolean mDownloadReceiverRegistered;

    public TermuxBrowserController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mWebView = activity.findViewById(R.id.browser_web_view);
        this.mBrowserContentContainer = activity.findViewById(R.id.browser_content_container);
        this.mBrowserTerminalDivider = activity.findViewById(R.id.browser_terminal_divider);
        this.mPageTitleUrlHeaderView = activity.findViewById(R.id.browser_page_title_url_header);
        this.mSwipeRefreshLayout = activity.findViewById(R.id.browser_swipe_refresh);
        this.mPageLoadProgressBar = activity.findViewById(R.id.browser_page_load_progress_bar);
        this.mWebViewCover = activity.findViewById(R.id.browser_web_view_cover);
        this.mTabsListView = activity.findViewById(R.id.browser_tabs_list);
        this.mProjectNameResolver = new BrowserProjectNameResolver(activity);
        this.mProjectOverviewActionsView = activity.findViewById(R.id.browser_project_overview_actions);
        configureWebView();
        configureCookies();
        configureDrawerControls();
        configureProjectOverviewActions();
        configureBrowserSplitDivider();
        configureHeaderInteractions();
    }

    private void configureHeaderInteractions() {
        mPageTitleUrlHeaderView.setOnClickListener(view -> promptEditCurrentPageUrl());
        mPageTitleUrlHeaderView.setOnLongClickListener(view -> copyCurrentPageUrlToClipboard());
    }

    private boolean copyCurrentPageUrlToClipboard() {
        String currentUrl = currentPageFullUrl();
        if (currentUrl == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return true;
        }
        ShareUtils.copyTextToClipboard(mActivity, currentUrl,
            mActivity.getString(R.string.msg_browser_url_copied));
        return true;
    }

    private void promptEditCurrentPageUrl() {
        if (mCurrentSessionHandle == null) return;
        String currentUrl = currentPageFullUrl();
        if (currentUrl == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_browser_edit_url);
        View dialogView = LayoutInflater.from(builder.getContext())
            .inflate(R.layout.dialog_browser_edit_url, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        EditText urlInput = dialogView.findViewById(R.id.browser_edit_url_input);
        urlInput.setText(currentUrl);
        Selection.setSelection(urlInput.getText(), currentUrl.length());

        Button goButton = dialogView.findViewById(R.id.browser_edit_url_go);
        Button copyButton = dialogView.findViewById(R.id.browser_edit_url_copy);
        Button createSessionButton = dialogView.findViewById(R.id.browser_edit_url_create_session);
        Button cancelButton = dialogView.findViewById(R.id.browser_edit_url_cancel);
        Button addBookmarkButton = dialogView.findViewById(R.id.browser_edit_url_add_bookmark);
        Button bookmarksButton = dialogView.findViewById(R.id.browser_edit_url_bookmarks);

        goButton.setText(R.string.action_browser_edit_url_confirm);
        copyButton.setText(R.string.action_browser_edit_url_copy);
        createSessionButton.setText(R.string.action_browser_edit_url_create_session);
        cancelButton.setText(android.R.string.cancel);
        addBookmarkButton.setText(R.string.action_browser_edit_url_add_bookmark);
        bookmarksButton.setText(R.string.action_browser_edit_url_bookmarks);

        goButton.setOnClickListener(view -> {
            navigateCurrentTabToUrl(urlInput.getText().toString());
            dialog.dismiss();
        });
        copyButton.setOnClickListener(view -> copyEditedUrlToClipboard(urlInput.getText().toString()));
        createSessionButton.setOnClickListener(view -> {
            createSessionForEditedUrl(urlInput.getText().toString());
            dialog.dismiss();
        });
        addBookmarkButton.setOnClickListener(view -> addCurrentPageBookmark());
        bookmarksButton.setOnClickListener(view -> {
            dialog.dismiss();
            showBookmarksList();
        });
        cancelButton.setOnClickListener(view -> dialog.dismiss());

        urlInput.setOnEditorActionListener((view, actionId, event) -> {
            navigateCurrentTabToUrl(urlInput.getText().toString());
            dialog.dismiss();
            return true;
        });

        dialog.show();
    }

    private void copyEditedUrlToClipboard(@Nullable String editedUrl) {
        String url = BrowserEditedUrl.trimmedOrNull(editedUrl);
        if (url == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        ShareUtils.copyTextToClipboard(mActivity, url,
            mActivity.getString(R.string.msg_browser_url_copied));
    }

    private void createSessionForEditedUrl(@Nullable String editedUrl) {
        String sessionName = BrowserEditedUrl.sessionNameFor(editedUrl);
        if (sessionName == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(sessionName);
    }

    private void addCurrentPageBookmark() {
        BrowserTab displayedTab = mRenderedFrame.getTab();
        String url = currentPageFullUrl();
        if (url == null || displayedTab == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        BrowserBookmark bookmark = new BrowserBookmark(url, displayedTab.getTitle());
        saveBookmarks(loadBookmarks().added(bookmark));
        mActivity.showToast(mActivity.getString(R.string.msg_browser_bookmarked), false);
    }

    private void showBookmarksList() {
        List<BrowserBookmark> bookmarks = loadBookmarks().getBookmarks();
        if (bookmarks.isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_bookmarks), false);
            return;
        }
        List<String> labels = new ArrayList<>();
        for (BrowserBookmark bookmark : bookmarks) {
            labels.add(bookmark.getTitle() + "\n" + bookmark.getUrl());
        }
        ListView listView = new ListView(mActivity);
        listView.setAdapter(new ArrayAdapter<>(mActivity, android.R.layout.simple_list_item_1, labels));
        AlertDialog dialog = new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_browser_bookmarks)
            .setView(listView)
            .create();
        dialog.setCanceledOnTouchOutside(true);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            openUrlInNewTab(bookmarks.get(position).getUrl());
            dialog.dismiss();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            promptDeleteBookmark(bookmarks.get(position));
            return true;
        });
        dialog.show();
    }

    private void promptDeleteBookmark(@NonNull BrowserBookmark bookmark) {
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_browser_bookmark_delete)
            .setMessage(bookmark.getUrl())
            .setPositiveButton(R.string.action_browser_bookmark_delete, (dialog, which) -> {
                saveBookmarks(loadBookmarks().removed(bookmark.getUrl()));
                showBookmarksList();
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> showBookmarksList()));
    }

    @NonNull
    private BrowserBookmarkCollection loadBookmarks() {
        try {
            return new BrowserBookmarkCollection(
                mBookmarkSerializer.deserialize(mActivity.getPreferences().getBrowserBookmarks()));
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to deserialize browser bookmarks, clearing the store", e);
            mActivity.getPreferences().setBrowserBookmarks(null);
            return new BrowserBookmarkCollection(new ArrayList<>());
        }
    }

    private void saveBookmarks(@NonNull BrowserBookmarkCollection bookmarks) {
        try {
            mActivity.getPreferences().setBrowserBookmarks(mBookmarkSerializer.serialize(bookmarks.getBookmarks()));
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize browser bookmarks", e);
        }
    }

    private void navigateCurrentTabToUrl(@Nullable String input) {
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) return;
        String targetUrl = normalizeUrl(input);
        activeTab.setUrl(targetUrl);
        renderFrame(activeTab);
        notifyTabsUpdated();
        mWebView.loadUrl(targetUrl);
    }

    @Nullable
    private String currentPageFullUrl() {
        BrowserTab displayedTab = mRenderedFrame.getTab();
        String displayedTabUrl = displayedTab == null ? null : displayedTab.getUrl();
        return BrowserCurrentPageUrl.fullUrl(displayedTabUrl, mRenderedFrame.getCommittedUrl());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureBrowserSplitDivider() {
        mBrowserTerminalDivider.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mSplitDragStartRawY = event.getRawY();
                    mSplitDragStartBrowserHeight = mBrowserContentContainer.getHeight();
                    mSplitDragTotalHeight = mSplitDragStartBrowserHeight + mActivity.getTerminalView().getHeight();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (mSplitDragTotalHeight <= 0) return true;
                    float draggedBrowserHeight = mSplitDragStartBrowserHeight + (event.getRawY() - mSplitDragStartRawY);
                    applyBrowserSplitRatio(draggedBrowserHeight / mSplitDragTotalHeight);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return false;
            }
        });
    }

    private void applyBrowserSplitRatio(float ratio) {
        float clampedRatio = BrowserSplitRatio.clamp(ratio);
        if (BrowserSplitRatio.isCollapsed(clampedRatio)) {
            showTerminal();
            return;
        }
        LinearLayout.LayoutParams browserParams =
            (LinearLayout.LayoutParams) mBrowserContentContainer.getLayoutParams();
        browserParams.height = 0;
        browserParams.weight = clampedRatio;
        mBrowserContentContainer.setLayoutParams(browserParams);
        View terminalView = mActivity.getTerminalView();
        LinearLayout.LayoutParams terminalParams =
            (LinearLayout.LayoutParams) terminalView.getLayoutParams();
        terminalParams.height = 0;
        terminalParams.weight = 1f - clampedRatio;
        terminalView.setLayoutParams(terminalParams);
        mLastAppliedBrowserSplitRatio = clampedRatio;
    }

    private void showBrowserSplitDivider() {
        applyBrowserSplitRatio(BrowserSplitRatio.resolveRatioToApply(mLastAppliedBrowserSplitRatio));
        mBrowserTerminalDivider.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = mWebView.getSettings();
        mDefaultUserAgent = BrowserUserAgent.normalizeDefault(settings.getUserAgentString());
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        applyDarkModeRendering(settings);
        BrowserWebAuthentication.apply(settings);
        BrowserWebViewAutofill.apply(mWebView, Build.VERSION.SDK_INT);

        mSwipeRefreshLayout.setOnRefreshListener(mWebView::reload);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (BrowserPageTransition.requiresCoverWhileLoading(
                        mRenderedFrame.getCommittedUrl(), url, mBrowserVisible)) {
                    showWebViewCover();
                }
                showPageLoadProgress(0);
                BrowserTab loadingTab = tabForUrlCallback(url, view.getUrl());
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    notifyTabsUpdated();
                }
                updatePageHeader();
                applyDesktopViewport(view, loadingTab);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                commitRenderedFrameUrl(url);
                revealWebView();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                commitRenderedFrameUrl(url);
                revealWebView();
                hidePageLoadProgress();
                mSwipeRefreshLayout.setRefreshing(false);
                CookieManager.getInstance().flush();
                BrowserTab loadingTab = tabForUrlCallback(url, view.getUrl());
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    loadingTab.setTitle(view.getTitle());
                    notifyTabsUpdated();
                }
                updatePageHeader();
                applyDesktopViewport(view, loadingTab);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                BrowserTab loadingTab = tabForUrlCallback(url, view.getUrl());
                if (loadingTab != null) {
                    loadingTab.setUrl(url);
                    notifyTabsUpdated();
                }
                updatePageHeader();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!request.isForMainFrame()) return;
                onMainFrameError();
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                onMainFrameError();
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                BrowserHttpAuthDialog.show(view.getContext(), handler, host, realm);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                BrowserPageLoadProgressState progressState =
                    BrowserPageLoadProgressState.forProgress(newProgress);
                if (progressState.isVisible()) {
                    showPageLoadProgress(progressState.getProgress());
                } else {
                    hidePageLoadProgress();
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                BrowserTab loadingTab = tabForTitleCallback(view.getUrl());
                if (loadingTab != null) {
                    loadingTab.setTitle(title);
                    notifyTabsUpdated();
                }
                updatePageHeader();
            }
        });

        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
            enqueueDownload(url, userAgent, contentDisposition, mimetype));

        new BrowserLinkContextMenuController(mActivity, mWebView, new BrowserLinkContextMenuController.Actions() {
            @Override
            public void openLinkInBrowser(@NonNull String linkUrl) {
                openUrlInNewTab(linkUrl);
            }

            @Override
            public void createSessionForLink(@NonNull String linkUrl) {
                mActivity.getTermuxTerminalSessionClient().addNewSessionApplyingAutosshConfig(linkUrl);
            }
        }).attach();
    }

    private void applyDarkModeRendering(@NonNull WebSettings settings) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int forceDarkMode = ThemeUtils.isNightModeEnabled(mActivity)
                ? WebSettingsCompat.FORCE_DARK_ON
                : WebSettingsCompat.FORCE_DARK_OFF;
            WebSettingsCompat.setForceDark(settings, forceDarkMode);
        }
    }

    private void onMainFrameError() {
        revealWebView();
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void showWebViewCover() {
        mWebViewCover.setVisibility(View.VISIBLE);
    }

    private void revealWebView() {
        mWebViewCover.setVisibility(View.GONE);
    }

    private void showPageLoadProgress(int progress) {
        if (!mBrowserVisible) return;
        mPageLoadProgressBar.setProgress(progress);
        mPageLoadProgressBar.setVisibility(View.VISIBLE);
    }

    private void hidePageLoadProgress() {
        mPageLoadProgressBar.setVisibility(View.GONE);
    }

    private void updatePageHeader() {
        boolean darkTheme =
            ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());
        int primaryColor = darkTheme ? Color.WHITE : Color.BLACK;
        mPageTitleUrlHeaderView.setTextColor(primaryColor);
        mPageTitleUrlHeaderView.setText(buildHeaderText(primaryColor));
        updateProjectOverviewActionsVisibility();
    }

    @NonNull
    private CharSequence buildHeaderText(int primaryColor) {
        BrowserTab displayedTab = mRenderedFrame.getTab();
        if (displayedTab == null) return "";
        String projectName = mProjectNameResolver.resolveProjectName(mCurrentSessionName);
        BrowserPageHeader header = BrowserPageHeaderText.build(
            projectName, mCurrentSessionName, displayedTab.getTitle(), displayedTab.getUrl(),
            isSessionHeaderVisible());

        SpannableStringBuilder builder = new SpannableStringBuilder();
        int secondaryColor = secondaryColor(primaryColor);
        if (header.hasContextLine()) {
            appendStyledLine(builder, header.getContextLine(), secondaryColor, true);
        }
        if (header.hasTitleLine()) {
            appendStyledLine(builder, header.getTitleLine(), primaryColor, false);
        }
        if (header.hasCompactUrlLine()) {
            appendStyledLine(builder, header.getCompactUrlLine(), secondaryColor, true);
        }
        return builder;
    }

    private boolean isSessionHeaderVisible() {
        return isViewVisible(R.id.session_name_bar) || isViewVisible(R.id.session_project_story_bar);
    }

    private boolean isViewVisible(int viewId) {
        View view = mActivity.findViewById(viewId);
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    private void appendStyledLine(@NonNull SpannableStringBuilder builder, @NonNull String text,
                                  int color, boolean secondary) {
        if (builder.length() > 0) builder.append('\n');
        int start = builder.length();
        builder.append(text);
        int end = builder.length();
        builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (secondary) {
            builder.setSpan(new RelativeSizeSpan(HEADER_SECONDARY_TEXT_SCALE), start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static int secondaryColor(int primaryColor) {
        return Color.argb(HEADER_SECONDARY_TEXT_ALPHA, Color.red(primaryColor),
            Color.green(primaryColor), Color.blue(primaryColor));
    }

    private void enqueueDownload(@Nullable String url, @Nullable String userAgent,
                                 @Nullable String contentDisposition, @Nullable String mimetype) {
        DownloadManager downloadManager =
            (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null || url == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
            return;
        }
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            if (!TextUtils.isEmpty(mimetype)) {
                request.setMimeType(mimetype);
            }
            if (!TextUtils.isEmpty(userAgent)) {
                request.addRequestHeader("User-Agent", userAgent);
            }
            String cookie = CookieManager.getInstance().getCookie(url);
            if (!TextUtils.isEmpty(cookie)) {
                request.addRequestHeader("Cookie", cookie);
            }
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            boolean receiverReady = registerDownloadCompleteReceiver();
            long downloadId = downloadManager.enqueue(request);
            if (receiverReady) {
                mEnqueuedDownloadIds.add(downloadId);
            }
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_started, fileName), false);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to enqueue browser download", e);
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
        }
    }

    private boolean registerDownloadCompleteReceiver() {
        if (mDownloadReceiverRegistered) return true;
        mDownloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (!mEnqueuedDownloadIds.remove(completedId)) return;
                if (isDownloadSuccessful(completedId)) {
                    openDownloadsView();
                } else {
                    mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        try {
            ContextCompat.registerReceiver(mActivity, mDownloadCompleteReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
            mDownloadReceiverRegistered = true;
            return true;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to register download receiver", e);
            mDownloadCompleteReceiver = null;
            return false;
        }
    }

    private void unregisterDownloadCompleteReceiver() {
        if (!mDownloadReceiverRegistered) return;
        try {
            mActivity.unregisterReceiver(mDownloadCompleteReceiver);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to unregister download receiver", e);
        }
        mDownloadReceiverRegistered = false;
    }

    private boolean isDownloadSuccessful(long downloadId) {
        DownloadManager downloadManager =
            (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) return false;
        try (Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor == null || !cursor.moveToFirst()) return false;
            int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusColumn < 0) return false;
            return cursor.getInt(statusColumn) == DownloadManager.STATUS_SUCCESSFUL;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to query download status", e);
            return false;
        }
    }

    private void openDownloadsView() {
        if (!mActivity.isVisible()) return;
        try {
            mActivity.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            mActivity.showToast(mActivity.getString(R.string.msg_browser_download_complete), false);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open downloads view", e);
        }
    }

    private void configureCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);
    }

    private void configureDrawerControls() {
        mActivity.findViewById(R.id.browser_new_tab_button).setOnClickListener(v -> promptNewTab());
        mActivity.findViewById(R.id.browser_open_in_chrome_button).setOnClickListener(v -> openCurrentPageInChrome());
        mActivity.findViewById(R.id.browser_send_page_text_button).setOnClickListener(v -> sendCurrentPageTextToTerminal());
        mActivity.findViewById(R.id.browser_clear_cache_button).setOnClickListener(v -> clearCurrentTabCache());
        mDesktopModeToggle = mActivity.findViewById(R.id.browser_desktop_mode_toggle);
        mDesktopModeToggle.setOnClickListener(v -> toggleActiveTabDesktopMode());
    }

    private void sendCurrentPageTextToTerminal() {
        if (!mBrowserVisible) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_session), false);
            return;
        }
        mWebView.evaluateJavascript(BrowserPageTextCapture.CAPTURE_SCRIPT, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String capturedTextJson) {
                String pageText;
                try {
                    pageText = BrowserPageTextCapture.parseCapturedText(capturedTextJson);
                } catch (JSONException e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to parse captured page text", e);
                    mActivity.showToast(mActivity.getString(R.string.msg_browser_page_text_failed), true);
                    return;
                }
                writeCapturedPageTextToSession(session, pageText);
            }
        });
    }

    private void writeCapturedPageTextToSession(@NonNull TerminalSession session, @NonNull String pageText) {
        File captureFile = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, BROWSER_PAGE_TEXT_FILE_NAME);
        try (FileOutputStream outputStream = new FileOutputStream(captureFile)) {
            outputStream.write(pageText.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to write captured page text", e);
            mActivity.showToast(mActivity.getString(R.string.msg_browser_page_text_failed), true);
            return;
        }
        session.write("cat " + captureFile.getAbsolutePath() + "\n");
        mActivity.showToast(mActivity.getString(R.string.msg_browser_page_text_sent), false);
    }

    private void clearCurrentTabCache() {
        if (!mBrowserVisible) return;
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) return;
        restampRenderedFrameNavigation();
        mWebView.clearCache(true);
        mWebView.reload();
        mActivity.showToast(mActivity.getString(R.string.msg_browser_cache_cleared), false);
    }

    private void openCurrentPageInChrome() {
        String currentUrl = mWebView.getUrl();
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        ShareUtils.openUrlInChrome(mActivity, currentUrl);
    }

    private void configureProjectOverviewActions() {
        mBulkOpenController = new BrowserBulkOpenController(mActivity, mWebView);
        mActivity.findViewById(R.id.browser_open_all_tasks_button)
            .setOnClickListener(v -> mBulkOpenController.openDisplayedTaskUrls(0));
        mActivity.findViewById(R.id.browser_open_first_ten_tasks_button)
            .setOnClickListener(v -> mBulkOpenController.openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT));
    }

    private void updateProjectOverviewActionsVisibility() {
        BrowserTab displayedTab = mRenderedFrame.getTab();
        boolean showActions = mBrowserVisible
            && displayedTab != null
            && BrowserProjectOverviewPage.isOverviewUrl(displayedTab.getUrl());
        mProjectOverviewActionsView.setVisibility(showActions ? View.VISIBLE : View.GONE);
    }

    private void updateSessionNameOverlay() {
        TermuxTerminalSessionActivityClient sessionClient = mActivity.getTermuxTerminalSessionClient();
        if (sessionClient != null) sessionClient.updateSessionNameOverlay();
    }

    private void toggleActiveTabDesktopMode() {
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            updateDesktopModeToggleState();
            return;
        }
        activeTab.setDesktopMode(!activeTab.isDesktopMode());
        applyUserAgent(activeTab);
        updateDesktopModeToggleState();
        renderFrame(activeTab);
        mWebView.reload();
    }

    private void applyUserAgent(@NonNull BrowserTab tab) {
        mWebView.getSettings().setUserAgentString(
            BrowserUserAgent.resolve(tab.isDesktopMode(), mDefaultUserAgent));
    }

    private void applyDesktopViewport(@NonNull WebView view, @Nullable BrowserTab tab) {
        if (!BrowserDesktopViewport.appliesTo(tab)) return;
        view.evaluateJavascript(BrowserDesktopViewport.INJECTION_SCRIPT, null);
    }

    private void updateDesktopModeToggleState() {
        if (mDesktopModeToggle == null) return;
        BrowserTab activeTab = getActiveTab();
        mDesktopModeToggle.setChecked(activeTab != null && activeTab.isDesktopMode());
    }

    public void onSessionChanged(@Nullable TerminalSession session) {
        String newSessionHandle = (session == null) ? null : session.mHandle;
        boolean switchingSession =
            BrowserSessionSwitch.requiresTerminalOnSessionChange(mCurrentSessionHandle, newSessionHandle);
        if (switchingSession) {
            mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, mBrowserVisible);
        }
        mCurrentSessionHandle = newSessionHandle;
        mCurrentSessionName = (session == null) ? null : session.mSessionName;
        rebindTabsList(session);
        updateDesktopModeToggleState();
        if (switchingSession) {
            restoreSessionVisibility();
            return;
        }
        if (mBrowserVisible) {
            if (getActiveTab() != null) {
                loadActiveTab();
            } else {
                showTerminal();
            }
        }
    }

    private void restoreSessionVisibility() {
        boolean restoreBrowser = mSessionVisibilityState.shouldRestoreBrowserOnSessionChange(
            mCurrentSessionHandle, getActiveTab() != null);
        if (restoreBrowser) {
            showBrowser();
        } else {
            showTerminal();
        }
    }

    private void rebindTabsList(@Nullable TerminalSession session) {
        if (mCurrentSessionHandle == null) {
            mTabsListView.setAdapter(null);
            mProjectNameResolver.loadEntriesForSession(null);
            return;
        }

        List<BrowserTab> tabs = mTabManager.getTabs(mCurrentSessionHandle);
        mTabsListViewController = new BrowserTabsListViewController(mActivity, this, tabs);
        mTabsListView.setAdapter(mTabsListViewController);
        mTabsListView.setOnItemClickListener(mTabsListViewController);

        String sessionName = (session == null) ? null : session.mSessionName;
        mProjectNameResolver.loadEntriesForSession(sessionName);
    }

    public void onSessionRemoved(@NonNull TerminalSession session) {
        if (session.mHandle.equals(mRenderedFrame.getOwnerSessionHandle()))
            blankFrame();
        mTabManager.removeSession(session.mHandle);
        mSessionVisibilityState.clearSession(session.mHandle);
    }

    public void toggleBrowser() {
        if (mBrowserVisible) {
            showTerminal();
        } else {
            showBrowser();
        }
    }

    public void showBrowser() {
        if (mCurrentSessionHandle == null) return;
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            promptNewTab();
            return;
        }
        mBrowserVisible = true;
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, true);
        loadActiveTab();
        updatePageHeader();
        mBrowserContentContainer.setVisibility(View.VISIBLE);
        showBrowserSplitDivider();
        dismissSoftKeyboardForBrowser();
        updateSessionNameOverlay();
    }

    private void dismissSoftKeyboardForBrowser() {
        EditText toolbarTextInput = mActivity.getTerminalToolbarTextInput();
        if (toolbarTextInput != null && toolbarTextInput.hasFocus())
            toolbarTextInput.clearFocus();
        View focusedView = mActivity.getCurrentFocus();
        View keyboardTargetView = focusedView != null ? focusedView : mActivity.getTerminalView();
        KeyboardUtils.hideSoftKeyboard(mActivity, keyboardTargetView);
    }

    public void showTerminal() {
        mBrowserVisible = false;
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, false);
        resetWebViewToBlankForForeignFrame();
        revealWebView();
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
        mBrowserContentContainer.setVisibility(View.GONE);
        mBrowserTerminalDivider.setVisibility(View.GONE);
        updateProjectOverviewActionsVisibility();
        updateSessionNameOverlay();
    }

    public boolean isBrowserVisible() {
        return mBrowserVisible;
    }

    @Nullable
    public String getDisplayedSessionHandle() {
        return mRenderedFrame.getOwnerSessionHandle();
    }

    public boolean hasBrowserTabForSession(@Nullable String sessionHandle) {
        return sessionHandle != null && mTabManager.getActiveTab(sessionHandle) != null;
    }

    public void reconcileDisplayedTabWithActiveSession(@Nullable TerminalSession session) {
        String activeSessionHandle = (session == null) ? null : session.mHandle;
        if (!java.util.Objects.equals(activeSessionHandle, mCurrentSessionHandle)) {
            onSessionChanged(session);
            return;
        }
        if (!mBrowserVisible) return;
        if (getActiveTab() != null) {
            loadActiveTab();
        } else {
            showTerminal();
        }
    }

    public void openTab(@NonNull BrowserTab tab) {
        boolean browserWasHidden = !mBrowserVisible;
        mTabManager.setActiveTab(tab);
        mBrowserVisible = true;
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, true);
        loadActiveTab(browserWasHidden);
        updatePageHeader();
        mBrowserContentContainer.setVisibility(View.VISIBLE);
        showBrowserSplitDivider();
        dismissSoftKeyboardForBrowser();
        notifyTabsUpdated();
        mActivity.getDrawer().closeDrawers();
        hideSessionListBottomSheet();
        updateSessionNameOverlay();
    }

    private void hideSessionListBottomSheet() {
        SessionListBottomSheetController.hideIfPresent(mActivity.getSessionListBottomSheetController());
    }

    public void closeTab(@NonNull BrowserTab tab) {
        mTabManager.removeTab(tab);
        if (mRenderedFrame.isDisplaying(tab)) blankFrame();
        notifyTabsUpdated();
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) {
            updateDesktopModeToggleState();
            showTerminal();
        } else if (mBrowserVisible) {
            loadActiveTab();
        } else {
            updateDesktopModeToggleState();
        }
    }

    public void openUrlInNewTab(@NonNull String url) {
        if (mCurrentSessionHandle == null) return;
        BrowserTab tab = mTabManager.addTab(mCurrentSessionHandle, normalizeUrl(url));
        openTab(tab);
    }

    public void attachBackgroundTab(@NonNull String sessionHandle, @NonNull String url) {
        mTabManager.attachOrActivateTab(sessionHandle, normalizeUrl(url));
        mSessionVisibilityState.setBrowserVisible(sessionHandle, true);
        if (sessionHandle.equals(mCurrentSessionHandle)) notifyTabsUpdated();
    }

    private void promptNewTab() {
        if (mCurrentSessionHandle == null) return;
        TextInputDialogUtils.textInput(mActivity, R.string.title_browser_open_url, null,
            R.string.action_browser_open_url_confirm, text -> {
                if (mCurrentSessionHandle == null) return;
                String url = normalizeUrl(text);
                BrowserTab tab = mTabManager.addTab(mCurrentSessionHandle, url);
                openTab(tab);
            },
            -1, null, android.R.string.cancel, null, null);
    }

    private static String normalizeUrl(@Nullable String input) {
        return BrowserUrlInput.normalize(input);
    }

    private void loadActiveTab() {
        loadActiveTab(false);
    }

    private void loadActiveTab(boolean forceReload) {
        BrowserTab activeTab = getActiveTab();
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            mCurrentSessionHandle, activeTab == null ? null : activeTab.getSessionHandle(),
            mRenderedFrame.getOwnerSessionHandle(),
            mRenderedFrame.getTab() == null ? null : mRenderedFrame.getTab().getSessionHandle(),
            forceReload);
        if (!resolution.shouldDisplay()) {
            resetWebViewToBlankForForeignFrame();
            return;
        }
        applyUserAgent(activeTab);
        updateDesktopModeToggleState();
        boolean sameTabAlreadyShown = mRenderedFrame.isDisplaying(activeTab);
        if (!resolution.shouldLoadWebView() && sameTabAlreadyShown) return;
        displayTabInWebView(activeTab);
    }

    private void displayTabInWebView(@NonNull BrowserTab tab) {
        String targetUrl = tab.getUrl();
        if (BrowserRenderedFrameOwnership.requiresCoverForFrame(
                mCurrentSessionHandle, mRenderedFrame.getOwnerSessionHandle(),
                mRenderedFrame.getCommittedUrl(), targetUrl, mBrowserVisible)) {
            showWebViewCover();
        }
        renderFrame(tab);
        mWebView.loadUrl(targetUrl);
    }

    private void renderFrame(@NonNull BrowserTab tab) {
        mRenderedFrame = BrowserRenderedFrame.renderingTab(tab);
        updatePageHeader();
    }

    private void blankFrame() {
        mRenderedFrame = BrowserRenderedFrame.BLANK;
    }

    private void commitRenderedFrameUrl(@Nullable String committedUrl) {
        mRenderedFrame = mRenderedFrame.withCommittedUrl(committedUrl);
    }

    private void restampRenderedFrameNavigation() {
        mRenderedFrame = mRenderedFrame.withRestampedNavigation();
    }

    private void resetWebViewToBlankForForeignFrame() {
        boolean renderedFrameIsForeign = BrowserRenderedFrameOwnership.isRenderedFrameForeign(
            mCurrentSessionHandle, mRenderedFrame.getOwnerSessionHandle());
        if (!renderedFrameIsForeign) return;
        blankFrame();
        mWebView.loadUrl("about:blank");
    }

    @Nullable
    private BrowserTab tabForUrlCallback(@Nullable String callbackUrl, @Nullable String webViewCurrentUrl) {
        BrowserWebViewNavigation inFlightNavigation = mRenderedFrame.getInFlightNavigation();
        if (inFlightNavigation == null) return null;
        return inFlightNavigation.tabForUrlCallback(
            mCurrentSessionHandle, mRenderedFrame.getOwnerSessionHandle(), callbackUrl, webViewCurrentUrl);
    }

    @Nullable
    private BrowserTab tabForTitleCallback(@Nullable String webViewCurrentUrl) {
        BrowserWebViewNavigation inFlightNavigation = mRenderedFrame.getInFlightNavigation();
        if (inFlightNavigation == null) return null;
        return inFlightNavigation.tabForTitleCallback(
            mCurrentSessionHandle, mRenderedFrame.getOwnerSessionHandle(), webViewCurrentUrl);
    }

    @Nullable
    public BrowserTab getActiveTab() {
        if (mCurrentSessionHandle == null) return null;
        return mTabManager.getActiveTab(mCurrentSessionHandle);
    }

    private void notifyTabsUpdated() {
        if (mTabsListViewController != null) mTabsListViewController.notifyDataSetChanged();
    }

    public boolean onBackPressed() {
        if (mBrowserVisible && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        if (mBrowserVisible) {
            showTerminal();
            return true;
        }
        return false;
    }

    public void toggleTabsDrawer() {
        DrawerLayout drawer = mActivity.getDrawer();
        if (drawer.isDrawerOpen(Gravity.END))
            drawer.closeDrawer(Gravity.END);
        else
            drawer.openDrawer(Gravity.END);
    }

    public void onActivityStop() {
        try {
            CookieManager.getInstance().flush();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to flush cookies", e);
        }
    }

    public void onActivityDestroy() {
        unregisterDownloadCompleteReceiver();
        mWebView.stopLoading();
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("about:blank");
        mWebView.removeAllViews();
        mWebView.destroy();
    }
}
