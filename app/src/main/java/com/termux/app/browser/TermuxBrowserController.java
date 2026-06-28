package com.termux.app.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.SessionListBottomSheetController;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;
import com.termux.shared.logger.Logger;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TermuxBrowserController implements BrowserTabSelectionListener {

    private static final String LOG_TAG = "TermuxBrowserController";

    public static final int REQUEST_BROWSER_FILE_CHOOSER = 3000;

    private static final String BROWSER_PAGE_TEXT_FILE_NAME = "browser-page.txt";

    private static final String BROWSER_SCREENSHOT_PNG_FILE_NAME = "browser-screenshot.png";

    private static final int BROWSER_SCREENSHOT_PNG_QUALITY = 100;

    private static final float HEADER_SECONDARY_TEXT_SCALE = 0.85f;

    private static final int HEADER_SECONDARY_TEXT_ALPHA = 0xB3;

    private final TermuxActivity mActivity;

    private final BrowserTabManager mTabManager = new BrowserTabManager();

    private final BrowserSessionVisibilityState mSessionVisibilityState = new BrowserSessionVisibilityState();

    private final BrowserOpenSessionNamesSerializer mOpenSessionNamesSerializer = new BrowserOpenSessionNamesSerializer();

    private final BrowserBookmarkSerializer mBookmarkSerializer = new BrowserBookmarkSerializer();

    private final BrowserUrlActions mUrlActions;

    private final BrowserPersistedTabsSerializer mPersistedTabsSerializer = new BrowserPersistedTabsSerializer();

    private final Map<String, BrowserPersistedSessionTabs> mPersistedTabsBySessionName = new LinkedHashMap<>();

    private final FrameLayout mWebViewContainer;

    private final BrowserTabWebViewHost mWebViewHost;

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

    private ImageButton mDesktopModeToggle;

    private String mDefaultUserAgent;

    private BrowserTabsListViewController mTabsListViewController;

    private BrowserTabFaviconStripController mTabFaviconStripController;

    private BrowserProjectActionButtonsController mProjectActionButtonsController;

    private final BrowserProjectNameResolver mProjectNameResolver;

    private final BrowserProjectActionUrlResolver mProjectActionUrlResolver;

    private final View mProjectOverviewActionsView;

    private BrowserBulkOpenController mBulkOpenController;

    private String mCurrentSessionHandle;

    private String mCurrentSessionName;

    private BrowserRenderedFrame mRenderedFrame = BrowserRenderedFrame.BLANK;

    private boolean mBrowserVisible;

    private final BrowserDownloadController mDownloadController = new BrowserDownloadController(
        new BrowserDownloadController.Host() {
            @NonNull
            @Override
            public Context getDownloadContext() {
                return mActivity;
            }

            @Override
            public boolean isActivityVisible() {
                return mActivity.isVisible();
            }

            @Override
            public void onDownloadStarted(@NonNull String fileName) {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_download_started, fileName), false);
            }

            @Override
            public void onDownloadComplete() {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_download_complete), false);
            }

            @Override
            public void onDownloadFailed() {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_download_failed), true);
            }
        });

    private ValueCallback<Uri[]> mPendingFileChooserCallback;

    public TermuxBrowserController(@NonNull TermuxActivity activity) {
        this.mActivity = activity;
        this.mWebViewContainer = activity.findViewById(R.id.browser_web_view_container);
        this.mWebViewHost = new BrowserTabWebViewHost(mWebViewContainer, this::createWebViewForTab);
        this.mBrowserContentContainer = activity.findViewById(R.id.browser_content_container);
        this.mBrowserTerminalDivider = activity.findViewById(R.id.browser_terminal_divider);
        this.mPageTitleUrlHeaderView = activity.findViewById(R.id.browser_page_title_url_header);
        this.mSwipeRefreshLayout = activity.findViewById(R.id.browser_swipe_refresh);
        this.mPageLoadProgressBar = activity.findViewById(R.id.browser_page_load_progress_bar);
        this.mWebViewCover = activity.findViewById(R.id.browser_web_view_cover);
        this.mTabsListView = activity.findViewById(R.id.browser_tabs_list);
        this.mProjectNameResolver = new BrowserProjectNameResolver(activity::getSessionDefinitionEntries);
        this.mProjectActionUrlResolver = new BrowserProjectActionUrlResolver(activity::getSessionDefinitionEntries);
        this.mProjectOverviewActionsView = activity.findViewById(R.id.browser_project_overview_actions);
        HorizontalScrollView tabStripScroll = activity.findViewById(R.id.browser_tab_strip_scroll);
        LinearLayout tabStripContainer = activity.findViewById(R.id.browser_tab_strip_container);
        mTabFaviconStripController = new BrowserTabFaviconStripController(tabStripScroll, tabStripContainer, this);
        mProjectActionButtonsController = new BrowserProjectActionButtonsController(
            activity.findViewById(R.id.browser_tab_bar_overview_button),
            activity.findViewById(R.id.browser_tab_bar_tdpm_console_button),
            activity.findViewById(R.id.browser_tab_bar_new_issue_button),
            this::openProjectActionUrl);
        this.mUrlActions = new BrowserUrlActions(mActivity, new BrowserUrlActions.Host() {
            @Override
            public String currentPageUrl() {
                return currentPageFullUrl();
            }

            @Override
            public void showNoCurrentUrlMessage() {
                mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            }

            @Override
            public void navigateToUrl(@NonNull String editedUrl) {
                navigateCurrentTabToUrl(editedUrl);
            }

            @Override
            public void createSessionForUrl(@NonNull String editedUrl) {
                createSessionForEditedUrl(editedUrl);
            }

            @Override
            public void addCurrentPageBookmark() {
                TermuxBrowserController.this.addCurrentPageBookmark();
            }

            @Override
            public void showBookmarksList() {
                TermuxBrowserController.this.showBookmarksList();
            }
        });
        configureWebView();
        configureCookies();
        configureDrawerControls();
        configureProjectOverviewActions();
        configureBrowserSplitDivider();
        configureHeaderInteractions();
        configureBrowserOpenStatePersistence();
        loadPersistedSessionTabs();
    }

    private void loadPersistedSessionTabs() {
        mPersistedTabsBySessionName.clear();
        try {
            List<BrowserPersistedSessionTabs> persistedSessionTabs =
                mPersistedTabsSerializer.deserialize(mActivity.getPreferences().getBrowserSessionTabs());
            for (BrowserPersistedSessionTabs sessionTabs : persistedSessionTabs) {
                mPersistedTabsBySessionName.put(sessionTabs.getSessionName(), sessionTabs);
            }
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load persisted browser session tabs", e);
        }
    }

    private void restorePersistedTabsForSession(@Nullable String sessionHandle, @Nullable String sessionName) {
        if (sessionHandle == null || sessionName == null || sessionName.isEmpty()) return;
        if (mTabManager.hasTabs(sessionHandle)) return;
        BrowserPersistedSessionTabs persistedSessionTabs = mPersistedTabsBySessionName.get(sessionName);
        if (persistedSessionTabs == null) return;
        mTabManager.restoreTabs(
            sessionHandle, persistedSessionTabs.getTabs(), persistedSessionTabs.getActiveTabIndex());
    }

    private void persistSessionTabs() {
        for (String sessionHandle : liveSessionHandlesWithName().keySet()) {
            String sessionName = liveSessionHandlesWithName().get(sessionHandle);
            if (sessionName == null || sessionName.isEmpty()) continue;
            List<BrowserTab> tabs = mTabManager.getTabs(sessionHandle);
            if (tabs.isEmpty()) {
                mPersistedTabsBySessionName.remove(sessionName);
                continue;
            }
            List<BrowserPersistedTab> persistedTabs = new ArrayList<>();
            for (BrowserTab tab : tabs) {
                persistedTabs.add(
                    new BrowserPersistedTab(tab.getUrl(), tab.getTitle(), tab.getViewMode().isDesktop()));
            }
            int activeTabIndex = mTabManager.getActiveTabIndex(sessionHandle);
            mPersistedTabsBySessionName.put(sessionName,
                new BrowserPersistedSessionTabs(sessionName, persistedTabs, Math.max(activeTabIndex, 0)));
        }
        writePersistedSessionTabs();
    }

    private void writePersistedSessionTabs() {
        try {
            mActivity.getPreferences().setBrowserSessionTabs(
                mPersistedTabsSerializer.serialize(new ArrayList<>(mPersistedTabsBySessionName.values())));
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to persist browser session tabs", e);
        }
    }

    @NonNull
    private Map<String, String> liveSessionHandlesWithName() {
        Map<String, String> liveSessionNamesByHandle = new LinkedHashMap<>();
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return liveSessionNamesByHandle;
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            liveSessionNamesByHandle.put(terminalSession.mHandle, terminalSession.mSessionName);
        }
        return liveSessionNamesByHandle;
    }

    private void configureBrowserOpenStatePersistence() {
        mSessionVisibilityState.setPersistedOpenSessionNames(
            mOpenSessionNamesSerializer.deserialize(mActivity.getPreferences().getBrowserOpenSessionNames()));
        mSessionVisibilityState.setPersistedNamesListener(openSessionNames ->
            mActivity.getPreferences().setBrowserOpenSessionNames(
                mOpenSessionNamesSerializer.serialize(openSessionNames)));
    }

    @Nullable
    private String resolveSessionName(@Nullable String sessionHandle) {
        if (sessionHandle == null) return null;
        if (sessionHandle.equals(mCurrentSessionHandle)) return mCurrentSessionName;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (sessionHandle.equals(terminalSession.mHandle)) return terminalSession.mSessionName;
        }
        return null;
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
        mUrlActions.promptEditCurrentPageUrl();
    }

    private void createSessionForEditedUrl(@Nullable String editedUrl) {
        String sessionName = BrowserEditedUrl.sessionNameFor(editedUrl);
        if (sessionName == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        mActivity.getTermuxTerminalSessionClient().addNewSessionForBrowserUrl(sessionName);
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
        WebView webView = mWebViewHost.showTab(activeTab);
        webView.loadUrl(targetUrl);
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

    private void configureWebView() {
        mSwipeRefreshLayout.setEnabled(false);
    }

    @Nullable
    private WebView currentWebView() {
        return mWebViewHost.getDisplayedWebView();
    }

    private void reloadDisplayedWebView() {
        WebView displayedWebView = currentWebView();
        if (displayedWebView != null) displayedWebView.reload();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @NonNull
    private WebView createWebViewForTab(@NonNull BrowserTab tab) {
        WebView webView = new WebView(mActivity);
        WebSettings settings = webView.getSettings();
        if (mDefaultUserAgent == null) {
            mDefaultUserAgent = BrowserUserAgent.normalizeDefault(settings.getUserAgentString());
        }
        BrowserWebViewConfigurator.apply(settings, tab.getViewMode(), mDefaultUserAgent);

        applyDarkModeRendering(settings);
        BrowserWebViewAutofill.apply(webView, Build.VERSION.SDK_INT);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new BrowserCoreWebViewClient(new BrowserCoreWebViewClient.Host() {
            @NonNull
            @Override
            public BrowserViewMode getViewMode() {
                return tab.getViewMode();
            }

            @Override
            public boolean shouldInjectMobileViewport() {
                return false;
            }

            @Override
            public void onPageStarted(@NonNull WebView view, @Nullable String url) {
                tab.setUrl(url);
                if (isDisplayedTab(tab)) {
                    if (BrowserPageTransition.requiresCoverWhileLoading(
                            mRenderedFrame.getCommittedUrl(), url, mBrowserVisible)) {
                        showWebViewCover();
                    }
                    showPageLoadProgress(0);
                    updatePageHeader();
                }
                notifyTabsUpdated();
            }

            @Override
            public void onPageCommitVisible(@NonNull WebView view, @Nullable String url) {
                if (!isDisplayedTab(tab)) return;
                commitRenderedFrameUrl(url);
                revealWebView();
            }

            @Override
            public boolean onPageFinished(@NonNull WebView view, @Nullable String url) {
                if (isDisplayedTab(tab) && mBrowserVisible && "about:blank".equals(url)) {
                    showTerminal();
                    return true;
                }
                tab.setUrl(url);
                tab.setTitle(view.getTitle());
                CookieManager.getInstance().flush();
                if (isDisplayedTab(tab)) {
                    commitRenderedFrameUrl(url);
                    revealWebView();
                    hidePageLoadProgress();
                    mSwipeRefreshLayout.setRefreshing(false);
                    updatePageHeader();
                }
                notifyTabsUpdated();
                persistSessionTabs();
                return false;
            }

            @Override
            public void onVisitedHistoryUpdated(@NonNull WebView view, @Nullable String url, boolean isReload) {
                tab.setUrl(url);
                if (isDisplayedTab(tab)) updatePageHeader();
                notifyTabsUpdated();
            }

            @Override
            public void onMainFrameError(@NonNull WebView view) {
                if (isDisplayedTab(tab)) handleMainFrameError();
            }

            @Override
            public boolean onRenderProcessGone(@NonNull WebView view, boolean didCrash) {
                return recoverFromRenderProcessGone(view, didCrash);
            }
        }));

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (!isDisplayedTab(tab)) return;
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
                tab.setTitle(title);
                if (isDisplayedTab(tab)) updatePageHeader();
                notifyTabsUpdated();
                persistSessionTabs();
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                if (icon == null) return;
                tab.setFavicon(icon);
                notifyTabsUpdated();
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                return showFileChooser(filePathCallback, fileChooserParams);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
            mDownloadController.enqueueDownload(url, userAgent, contentDisposition, mimetype));

        new BrowserLinkContextMenuController(mActivity, webView, new BrowserLinkContextMenuController.Actions() {
            @Override
            public void openLinkInBrowser(@NonNull String linkUrl) {
                openUrlInNewTab(linkUrl);
            }

            @Override
            public void createSessionForLink(@NonNull String linkUrl) {
                mActivity.getTermuxTerminalSessionClient().addNewSessionForBrowserUrl(linkUrl);
            }
        }).attach();

        return webView;
    }

    private boolean isDisplayedTab(@NonNull BrowserTab tab) {
        return mWebViewHost.getDisplayedTab() == tab;
    }

    private boolean showFileChooser(@Nullable ValueCallback<Uri[]> filePathCallback,
                                    @Nullable WebChromeClient.FileChooserParams fileChooserParams) {
        if (filePathCallback == null) {
            return false;
        }
        cancelPendingFileChooser();
        Intent intent = buildFileChooserIntent(fileChooserParams);
        try {
            mPendingFileChooserCallback = filePathCallback;
            mActivity.startActivityForResult(intent, REQUEST_BROWSER_FILE_CHOOSER);
            return true;
        } catch (ActivityNotFoundException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to open file chooser", e);
            mPendingFileChooserCallback = null;
            filePathCallback.onReceiveValue(null);
            return false;
        }
    }

    @NonNull
    private Intent buildFileChooserIntent(@Nullable WebChromeClient.FileChooserParams fileChooserParams) {
        if (fileChooserParams != null) {
            boolean allowMultiple = fileChooserParams.getMode()
                == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
            return BrowserFileChooserResult.buildIntent(allowMultiple, fileChooserParams.getAcceptTypes());
        }
        return BrowserFileChooserResult.buildIntent(false, null);
    }

    public void deliverFileChooserResult(int resultCode, @Nullable Intent data) {
        ValueCallback<Uri[]> callback = mPendingFileChooserCallback;
        mPendingFileChooserCallback = null;
        if (callback == null) {
            return;
        }
        callback.onReceiveValue(BrowserFileChooserResult.parse(resultCode, data));
    }

    private void cancelPendingFileChooser() {
        ValueCallback<Uri[]> callback = mPendingFileChooserCallback;
        mPendingFileChooserCallback = null;
        if (callback != null) {
            callback.onReceiveValue(null);
        }
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

    private void handleMainFrameError() {
        revealWebView();
        hidePageLoadProgress();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private boolean recoverFromRenderProcessGone(@NonNull WebView deadWebView, boolean didCrash) {
        BrowserTab tab = mWebViewHost.findTabForWebView(deadWebView);
        BrowserRenderProcessGoneDecision decision = BrowserRenderProcessGoneDecision.forDiedWebView(
            tab != null, tab != null && isDisplayedTab(tab), didCrash);
        if (!decision.shouldRecreateWebView()) return false;
        Logger.logWarn(LOG_TAG, "Browser WebView renderer process gone (didCrash=" + didCrash + "); recreating tab WebView");
        WebView recreatedWebView = mWebViewHost.recreateWebViewForTab(tab);
        if (decision.shouldNotifyUser()) {
            revealWebView();
            hidePageLoadProgress();
            mSwipeRefreshLayout.setRefreshing(false);
            recreatedWebView.requestFocus();
            Logger.showToast(mActivity, mActivity.getString(R.string.msg_browser_render_process_gone), false);
        }
        return true;
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

    private void configureCookies() {
        CookieManager.getInstance().setAcceptCookie(true);
    }

    private void configureDrawerControls() {
        mActivity.findViewById(R.id.browser_new_tab_button).setOnClickListener(v -> promptNewTab());
        mActivity.findViewById(R.id.browser_open_in_chrome_button).setOnClickListener(v -> openCurrentPageInChrome());
        mActivity.findViewById(R.id.browser_send_page_text_button).setOnClickListener(v -> sendCurrentPageTextToTerminal());
        mActivity.findViewById(R.id.browser_send_screenshot_button).setOnClickListener(v -> sendCurrentScreenshotToTerminal());
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
        WebView displayedWebView = currentWebView();
        if (displayedWebView == null) return;
        displayedWebView.evaluateJavascript(BrowserPageTextCapture.CAPTURE_SCRIPT, new ValueCallback<String>() {
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

    private void sendCurrentScreenshotToTerminal() {
        if (!mBrowserVisible) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_session), false);
            return;
        }
        WebView displayedWebView = currentWebView();
        if (displayedWebView == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_screenshot_failed), true);
            return;
        }
        int width = displayedWebView.getWidth();
        int height = displayedWebView.getHeight();
        if (width <= 0 || height <= 0) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_screenshot_failed), true);
            return;
        }
        byte[] pngBytes = renderVisibleWebViewToPng(displayedWebView, width, height);
        if (pngBytes == null) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_screenshot_failed), true);
            return;
        }
        writeCapturedScreenshotToSession(session, pngBytes);
    }

    @Nullable
    private byte[] renderVisibleWebViewToPng(@NonNull WebView webView, int width, int height) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            webView.draw(canvas);
            ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, BROWSER_SCREENSHOT_PNG_QUALITY, pngStream);
            return pngStream.toByteArray();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to render browser screenshot", e);
            return null;
        } finally {
            if (bitmap != null) bitmap.recycle();
        }
    }

    private void writeCapturedScreenshotToSession(@NonNull TerminalSession session, @NonNull byte[] pngBytes) {
        session.write(BrowserScreenshotCapture.buildDeliveryCommand(
            pngBytes, BROWSER_SCREENSHOT_PNG_FILE_NAME));
        mActivity.showToast(mActivity.getString(R.string.msg_browser_screenshot_sent), false);
    }

    private void clearCurrentTabCache() {
        if (!mBrowserVisible) return;
        BrowserTab activeTab = getActiveTab();
        if (activeTab == null) return;
        WebView displayedWebView = currentWebView();
        if (displayedWebView == null) return;
        displayedWebView.clearCache(true);
        displayedWebView.reload();
        mActivity.showToast(mActivity.getString(R.string.msg_browser_cache_cleared), false);
    }

    private void openCurrentPageInChrome() {
        WebView displayedWebView = currentWebView();
        String currentUrl = displayedWebView == null ? null : displayedWebView.getUrl();
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            mActivity.showToast(mActivity.getString(R.string.msg_browser_no_current_url), false);
            return;
        }
        ShareUtils.openUrlInChrome(mActivity, currentUrl);
    }

    private void configureProjectOverviewActions() {
        mBulkOpenController = new BrowserBulkOpenController(mActivity);
        mActivity.findViewById(R.id.browser_open_all_tasks_button)
            .setOnClickListener(v -> openDisplayedTaskUrls(0));
        mActivity.findViewById(R.id.browser_open_first_ten_tasks_button)
            .setOnClickListener(v -> openDisplayedTaskUrls(BrowserGithubTaskUrls.OPEN_FIRST_N_LIMIT));
    }

    private void openDisplayedTaskUrls(int limit) {
        WebView displayedWebView = currentWebView();
        if (displayedWebView == null) return;
        mBulkOpenController.openDisplayedTaskUrls(displayedWebView, limit);
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
        activeTab.setViewMode(BrowserViewMode.forDesktopFlag(!activeTab.getViewMode().isDesktop()));
        applyUserAgent(activeTab);
        updateDesktopModeToggleState();
        renderFrame(activeTab);
        reloadDisplayedWebView();
        persistSessionTabs();
    }

    private void applyUserAgent(@NonNull BrowserTab tab) {
        WebView displayedWebView = currentWebView();
        if (displayedWebView == null) return;
        displayedWebView.getSettings().setUserAgentString(
            BrowserUserAgent.resolve(tab.getViewMode().isDesktop(), mDefaultUserAgent));
    }

    private void updateDesktopModeToggleState() {
        if (mDesktopModeToggle == null) return;
        BrowserTab activeTab = getActiveTab();
        boolean isDesktop = activeTab != null && activeTab.getViewMode().isDesktop();
        if (isDesktop) {
            mDesktopModeToggle.setColorFilter(0xFF03A9F4);
        } else {
            mDesktopModeToggle.clearColorFilter();
        }
    }

    public void onSessionChanged(@Nullable TerminalSession session) {
        String newSessionHandle = (session == null) ? null : session.mHandle;
        boolean switchingSession =
            BrowserSessionSwitch.requiresTerminalOnSessionChange(mCurrentSessionHandle, newSessionHandle);
        mCurrentSessionHandle = newSessionHandle;
        mCurrentSessionName = (session == null) ? null : session.mSessionName;
        restorePersistedTabsForSession(mCurrentSessionHandle, mCurrentSessionName);
        rebindTabsList();
        updateDesktopModeToggleState();
        updateProjectActionButtons();
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
        boolean hasActiveTab = getActiveTab() != null;
        boolean restoreBrowser = mSessionVisibilityState.shouldRestoreBrowserOnSessionChange(
            mCurrentSessionHandle, hasActiveTab)
            || (hasActiveTab && mSessionVisibilityState.wasBrowserOpenForSessionName(mCurrentSessionName));
        if (restoreBrowser) {
            showBrowser();
        } else {
            showTerminal();
        }
    }

    private void rebindTabsList() {
        if (mCurrentSessionHandle == null) {
            mTabsListView.setAdapter(null);
            return;
        }

        List<BrowserTab> tabs = mTabManager.getTabs(mCurrentSessionHandle);
        mTabsListViewController = new BrowserTabsListViewController(mActivity, this, tabs);
        mTabsListView.setAdapter(mTabsListViewController);
        mTabsListView.setOnItemClickListener(mTabsListViewController);
    }

    public void onSessionRemoved(@NonNull TerminalSession session) {
        if (session.mHandle.equals(mRenderedFrame.getOwnerSessionHandle()))
            blankFrame();
        mWebViewHost.removeSession(session.mHandle);
        mTabManager.removeSession(session.mHandle);
        mSessionVisibilityState.clearSession(session.mHandle, session.mSessionName);
        if (session.mSessionName != null && !session.mSessionName.isEmpty()) {
            mPersistedTabsBySessionName.remove(session.mSessionName);
            writePersistedSessionTabs();
        }
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
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, mCurrentSessionName, true);
        loadActiveTab();
        updatePageHeader();
        updateProjectActionButtons();
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
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, mCurrentSessionName, false);
        hideBrowserViews();
    }

    public void hideBrowserForSessionOverlay() {
        hideBrowserViews();
    }

    private void hideBrowserViews() {
        mBrowserVisible = false;
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
        mSessionVisibilityState.setBrowserVisible(mCurrentSessionHandle, mCurrentSessionName, true);
        loadActiveTab(browserWasHidden);
        updatePageHeader();
        mBrowserContentContainer.setVisibility(View.VISIBLE);
        showBrowserSplitDivider();
        dismissSoftKeyboardForBrowser();
        notifyTabsUpdated();
        mActivity.getDrawer().closeDrawers();
        hideSessionListBottomSheet();
        updateSessionNameOverlay();
        persistSessionTabs();
    }

    private void hideSessionListBottomSheet() {
        SessionListBottomSheetController.hideIfPresent(mActivity.getSessionListBottomSheetController());
    }

    public void closeTab(@NonNull BrowserTab tab) {
        mTabManager.removeTab(tab);
        mWebViewHost.removeTab(tab);
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
        persistSessionTabs();
    }

    public void openUrlInNewTab(@NonNull String url) {
        String sessionHandle = mCurrentSessionHandle;
        if (sessionHandle == null) {
            TerminalSession currentSession = mActivity.getCurrentSession();
            if (currentSession == null) return;
            sessionHandle = currentSession.mHandle;
        }
        BrowserTab tab = mTabManager.addTab(sessionHandle, normalizeUrl(url));
        openTab(tab);
    }

    public void openUrlInNewTab(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        String sessionHandle = mCurrentSessionHandle;
        if (sessionHandle == null) {
            TerminalSession currentSession = mActivity.getCurrentSession();
            if (currentSession == null) return;
            sessionHandle = currentSession.mHandle;
        }
        BrowserTab tab = mTabManager.addTab(sessionHandle, normalizeUrl(url));
        tab.setViewMode(viewMode);
        openTab(tab);
    }

    private void openProjectActionUrl(@NonNull String url, @NonNull BrowserViewMode viewMode) {
        openUrlInNewTab(url, viewMode);
    }

    private void updateProjectActionButtons() {
        if (mProjectActionButtonsController == null) return;
        mProjectActionButtonsController.setActionUrls(
            mProjectActionUrlResolver.resolveForSessionName(mCurrentSessionName));
    }

    public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url) {
        BrowserTab tab = mTabManager.addTab(sessionHandle, normalizeUrl(url));
        if (sessionHandle.equals(mCurrentSessionHandle)) {
            openTab(tab);
            return;
        }
        mSessionVisibilityState.setBrowserVisible(sessionHandle, resolveSessionName(sessionHandle), true);
        persistSessionTabs();
    }

    public void attachBackgroundTab(@NonNull String sessionHandle, @NonNull String url) {
        restorePersistedTabsForSession(sessionHandle, resolveSessionName(sessionHandle));
        mTabManager.attachOrActivateTab(sessionHandle, normalizeUrl(url));
        mSessionVisibilityState.setBrowserVisible(sessionHandle, resolveSessionName(sessionHandle), true);
        if (sessionHandle.equals(mCurrentSessionHandle)) notifyTabsUpdated();
        persistSessionTabs();
    }

    public void promptNewTab() {
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
        if (activeTab == null) {
            blankFrame();
            return;
        }
        updateDesktopModeToggleState();
        displayTab(activeTab, forceReload);
    }

    private void displayTab(@NonNull BrowserTab tab, boolean forceReload) {
        boolean firstDisplay = !mWebViewHost.hasWebViewForTab(tab);
        if (firstDisplay) showWebViewCover();
        renderFrame(tab);
        WebView webView = mWebViewHost.showTab(tab);
        if (forceReload && !firstDisplay) webView.reload();
        else if (!firstDisplay) revealWebView();
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

    @Nullable
    public BrowserTab getActiveTab() {
        if (mCurrentSessionHandle == null) return null;
        return mTabManager.getActiveTab(mCurrentSessionHandle);
    }

    private void notifyTabsUpdated() {
        if (mTabsListViewController != null) mTabsListViewController.notifyDataSetChanged();
        if (mTabFaviconStripController != null && mCurrentSessionHandle != null) {
            mTabFaviconStripController.update(
                mTabManager.getTabs(mCurrentSessionHandle),
                getActiveTab()
            );
        }
    }

    public boolean onBackPressed() {
        WebView displayedWebView = currentWebView();
        if (mBrowserVisible && displayedWebView != null && displayedWebView.canGoBack()) {
            WebBackForwardList backForwardList = displayedWebView.copyBackForwardList();
            int previousIndex = backForwardList.getCurrentIndex() - 1;
            if (previousIndex >= 0 && "about:blank".equals(backForwardList.getItemAtIndex(previousIndex).getUrl())) {
                showTerminal();
            } else {
                displayedWebView.goBack();
            }
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
        cancelPendingFileChooser();
        mDownloadController.unregisterDownloadCompleteReceiver();
        mWebViewHost.destroyAll();
    }
}
