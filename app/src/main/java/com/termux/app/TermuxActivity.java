package com.termux.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.termux.R;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.apkupdate.ApkUpdateFloatingIndicatorController;
import com.termux.app.apkupdate.ApkUpdateForegroundCheckThrottle;
import com.termux.app.apkupdate.ApkUpdateUiController;
import com.termux.app.apkupdate.UpdateTagUpdateController;
import com.termux.app.apkupdate.UpdateTagUpdateRunner;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.tts.TtsManager;
import com.termux.app.appopen.AppOpenTagController;
import com.termux.app.appopen.InstalledAppLauncher;
import com.termux.app.browser.BrowserInboundViewUrl;
import com.termux.app.link.NativeAppLink;
import com.termux.app.link.OpenTagUrlNativeAppOpener;
import com.termux.app.browser.OpenTagBrowserController;
import com.termux.app.diagnostics.ActivityWindowRecorderHolder;
import com.termux.app.diagnostics.WindowDrawTimeObserver;
import com.termux.app.diagnostics.WindowDrawTimeRecorderHolder;
import com.termux.app.diagnostics.TermuxActivityHolder;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.shared.interact.ShareUtils;
import com.termux.app.ownercall.OwnerCallBodySpannedText;
import com.termux.app.ownercall.OwnerCallDialogController;
import com.termux.app.ownercall.OwnerCallDialogGeometry;
import com.termux.app.ownercall.OwnerCallDialogPlacement;
import com.termux.app.ownercall.OwnerCallDialogRelayoutWatcher;
import com.termux.app.ownercall.OwnerCallDialogStoredPlacement;
import com.termux.app.ownercall.OwnerCallDialogViewport;
import com.termux.app.ownercall.OwnerCallFileUrl;
import com.termux.app.ownercall.OwnerCallInbox;
import com.termux.app.sessiondefinition.SessionDefinitionAutoReloadScheduler;
import com.termux.app.sessiondefinition.SessionReconnectScheduler;
import com.termux.app.sessiondefinition.SessionDefinitionController;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.app.sessiondefinition.SessionDefinitionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionRepository;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.terminal.CallToUserTagController;
import com.termux.app.terminal.ExpandedProjectsAllowlistParser;
import com.termux.app.terminal.ProjectActionToken;
import com.termux.app.terminal.ProjectActionTokenParser;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.SessionListBottomSheetController;
import com.termux.app.terminal.CallingSessionNavigator;
import com.termux.app.terminal.CallingSessionSplit;
import com.termux.app.terminal.SessionActivityDirection;
import com.termux.app.terminal.SessionNavigationButtonsBinder;
import com.termux.app.terminal.SessionSwitchPickerController;
import com.termux.app.terminal.session.SessionDefinitionPrewarm;
import com.termux.app.terminal.session.SessionEagerLoadPacer;
import com.termux.app.terminal.session.SessionEagerLoader;
import com.termux.app.terminal.TermuxSessionsListViewController;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.termux.extrakeys.ExtraKeysView;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.shared.theme.NightMode;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class TermuxActivity extends AppCompatActivity implements ServiceConnection {

    /**
     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TermuxService mTermuxService;

    /**
     * The {@link TerminalView} shown in  {@link TermuxActivity} that displays the terminal.
     */
    TerminalView mTerminalView;

    /**
     *  The {@link TerminalViewClient} interface implementation to allow for communication between
     *  {@link TerminalView} and {@link TermuxActivity}.
     */
    TermuxTerminalViewClient mTermuxTerminalViewClient;

    /**
     *  The {@link TerminalSessionClient} interface implementation to allow for communication between
     *  {@link TerminalSession} and {@link TermuxActivity}.
     */
    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /**
     * Reads aloud text enclosed in literal {@code <speak>...</speak>} tags detected in session output.
     */
    private TtsManager mTtsManager;

    /**
     * Termux app shared preferences manager.
     */
    private TermuxAppSharedPreferences mPreferences;

    /**
     * Termux app SharedProperties loaded from termux.properties
     */
    private TermuxAppSharedProperties mProperties;

    /**
     * The root view of the {@link TermuxActivity}.
     */
    TermuxActivityRootView mTermuxActivityRootView;

    /**
     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxActivity}.
     */
    View mTermuxActivityBottomSpaceView;

    /**
     * The terminal extra keys view.
     */
    ExtraKeysView mExtraKeysView;

    /**
     * The client for the {@link #mExtraKeysView}.
     */
    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;

    /**
     * The termux sessions list controller.
     */
    TermuxSessionsListViewController mTermuxSessionListViewController;

    public static final String EXTRA_EXPANDED_PROJECTS = "expanded_projects";

    private List<String> mPendingExpandedProjectsAllowlist = Collections.emptyList();

    private List<ProjectActionToken> mPendingProjectActionTokens = Collections.emptyList();

    @Nullable
    private String mPendingInboundBrowserUrl;

    SessionListBottomSheetController mSessionListBottomSheetController;

    SessionSwitchPickerController mSessionSwitchPickerController;

    private ImageButton mPreviousSessionButton;

    private ImageButton mNextSessionButton;

    private TextView mPreviousSessionCountBadge;

    private TextView mNextSessionCountBadge;

    private ImageButton mJumpToCallingSessionButton;

    private TextView mJumpToCallingSessionCountBadge;

    private final SessionDefinitionRepository mSessionDefinitionRepository =
        new SessionDefinitionRepository();

    private final OwnerCallInbox mOwnerCallInbox = new OwnerCallInbox();

    private OwnerCallDialogController mOwnerCallDialogController;

    @Nullable
    public OwnerCallDialogController getOwnerCallDialogController() {
        return mOwnerCallDialogController;
    }

    private final SessionDefinitionPrewarm mSessionDefinitionPrewarm = new SessionDefinitionPrewarm(
        new SessionDefinitionPrewarm.DocumentLoadState() {
            @Override
            public boolean isDocumentLoaded() {
                return mSessionDefinitionRepository.isLoaded();
            }

            @Override
            public boolean isDocumentLoading() {
                return mSessionDefinitionRepository.isLoading();
            }
        },
        () -> getPreferences().getSessionDefinitionUrl(),
        baseUrl -> mSessionDefinitionRepository.load(baseUrl, this::onSessionDefinitionDocumentPrewarmed));

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final WindowDrawTimeObserver mWindowDrawTimeObserver =
        new WindowDrawTimeObserver(WindowDrawTimeRecorderHolder.getInstance());

    private final SessionDefinitionAutoReloadScheduler mSessionDefinitionAutoReloadScheduler =
        new SessionDefinitionAutoReloadScheduler(this::loadSessionsFromDefinition);

    private final SessionReconnectScheduler mSessionReconnectScheduler =
        new SessionReconnectScheduler(this::reconnectDeadDisplayedSessions);

    private final SessionEagerLoadPacer mSessionEagerLoadPacer = new SessionEagerLoadPacer(
        this::collectSessionsToEagerLoad,
        mMainThreadHandler::postDelayed,
        this::eagerLoadSessionEmulator);

    private final SessionEagerLoader mSessionEagerLoader = new SessionEagerLoader(
        this::collectSessionsToEagerLoad,
        session -> session.getEmulator() != null,
        mSessionEagerLoadPacer::enqueueSession);

    /**
     * The in-app browser controller managing the {@link android.webkit.WebView} and per-session tabs.
     */
    TermuxBrowserController mTermuxBrowserController;

    /**
     * The foreground URL opener for `<open>...</open>` tags. Opening a tab is a foreground action, so
     * this opener is registered with the service-owned {@link OpenTagBrowserController} only while the
     * activity is bound; the controller itself (and its per-session deduplication state) lives in
     * {@link TermuxService} so an already-opened tag still visible in the transcript does not re-fire
     * and spawn duplicate tabs when this activity is recreated.
     */
    OpenTagBrowserController.UrlOpener mOpenTagUrlOpener;

    AppOpenTagController.AppLauncher mAppLauncher;

    /**
     * The foreground update-flow trigger. When an update tag is detected it auto-downloads the APK in
     * the background and surfaces the floating install button rather than blocking on a dialog. It
     * requires this activity for its UI, so it is registered with the service-owned
     * {@link UpdateTagUpdateController} only while the activity is bound; the controller itself lives in
     * {@link TermuxService} so the update tag is detected for every session even when this activity is
     * not the one currently shown.
     */
    UpdateTagUpdateRunner mUpdateTagUpdateRunner;

    /**
     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

    private List<ActivityComponent> mActivityComponents = Collections.emptyList();

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    Toast mLastToast;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    private boolean mIsVisible;

    /**
     * If onResume() was called after onCreate().
     */
    private boolean mIsOnResumeAfterOnCreate = false;

    /**
     * If activity was restarted like due to call to {@link #recreate()} after receiving
     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity
     * was killed by android.
     */
    private boolean mIsActivityRecreated = false;

    /**
     * The {@link TermuxActivity} is in an invalid state and must not be run.
     */
    private boolean mIsInvalidState;

    private int mNavBarHeight;

    private float mTerminalToolbarDefaultHeight;


    private static final int REQUEST_POST_NOTIFICATIONS = 3001;

    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_SEND_SELECTED_TEXT_TO_TERMINAL = 12;
    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 11;
    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 2;
    private static final int CONTEXT_MENU_TRANSLATE_SELECTED_TEXT = 13;
    private static final int CONTEXT_MENU_OPEN_LINK_IN_BROWSER_ID = 14;
    private static final int CONTEXT_MENU_OPEN_LINK_IN_CHROME_ID = 15;
    private static final int CONTEXT_MENU_COPY_LINK_URL_ID = 16;
    static final int CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID = 17;
    private static final int CONTEXT_MENU_OPEN_LINK_IN_BROWSER_BACKGROUND_ID = 18;

    static final String GOOGLE_TRANSLATE_PACKAGE_NAME = "com.google.android.apps.translate";

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    private static final String LOG_TAG = "TermuxActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
        ActivityWindowRecorderHolder.getInstance().recordActivityCreated();
        mIsOnResumeAfterOnCreate = true;

        if (savedInstanceState != null)
            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);

        // Delete ReportInfo serialized object files from cache older than 14 days
        ReportActivity.deleteReportInfoFilesOlderThanXDays(this, 14, false);

        // Load Termux app SharedProperties from disk
        mProperties = TermuxAppSharedProperties.getProperties();
        reloadProperties();

        setActivityTheme();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_termux);

        mWindowDrawTimeObserver.observe(getWindow().getDecorView());

        // Load termux shared preferences
        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxAppSharedPreferences.build(this, true);
        if (mPreferences == null) {
            // An AlertDialog should have shown to kill the app, so we don't continue running activity code
            mIsInvalidState = true;
            return;
        }

        setMargins();

        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);
        mTermuxActivityRootView.setActivity(this);
        mTermuxActivityBottomSpaceView = findViewById(R.id.activity_termux_bottom_space_view);
        mTermuxActivityRootView.setOnApplyWindowInsetsListener(new TermuxActivityRootView.WindowInsetsListener());

        View content = findViewById(android.R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            mNavBarHeight = insets.getSystemWindowInsetBottom();
            return insets;
        });

        if (mProperties.isUsingFullScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mTtsManager = new TtsManager(this);

        setTermuxTerminalViewAndClients();

        setTerminalToolbarView(savedInstanceState);

        setBrowserView();

        setBrowserToggleBarView();

        setKeyboardToggleBarView();

        setRightDrawerToggleBarView();

        setSessionSheetToggleBarView();

        mSessionSwitchPickerController = new SessionSwitchPickerController(this);

        setSessionNavigationButtonsView();

        buildActivityComponents();

        registerForContextMenu(mTerminalView);

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        try {
            // Start the {@link TermuxService} and make it run regardless of who is bound to it
            Intent serviceIntent = new Intent(this, TermuxService.class);
            startService(serviceIntent);

            // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}
            // callback if it succeeds.
            if (!bindService(serviceIntent, this, 0))
                throw new RuntimeException("bindService() failed");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG,"TermuxActivity failed to start TermuxService", e);
            Logger.showToast(this,
                getString(e.getMessage() != null && e.getMessage().contains("app is in background") ?
                    R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),
                true);
            mIsInvalidState = true;
            return;
        }

        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that Termux
        // app has been opened.
        TermuxUtils.sendTermuxOpenedBroadcast(this);

        checkForApkUpdateAndShowIndicator();
    }

    private void buildActivityComponents() {
        ActivityComponent sessionListBottomSheetComponent = new ActivityComponent() {
            @Override
            public boolean onBackPressed() {
                if (mSessionListBottomSheetController.isOpen()) {
                    mSessionListBottomSheetController.hide();
                    return true;
                }
                return false;
            }
        };

        ActivityComponent rightDrawerComponent = new ActivityComponent() {
            @Override
            public boolean onBackPressed() {
                if (getDrawer().isDrawerOpen(Gravity.RIGHT)) {
                    getDrawer().closeDrawers();
                    return true;
                }
                return false;
            }
        };

        ActivityComponent browserComponent = new ActivityComponent() {
            @Override
            public boolean onBackPressed() {
                return mTermuxBrowserController.onBackPressed();
            }

            @Override
            public void onActivityResume() {
                mTermuxBrowserController.onActivityResume();
            }

            @Override
            public void onActivityStop() {
                mTermuxBrowserController.onActivityStop();
            }

            @Override
            public void onActivityDestroy() {
                mTermuxBrowserController.onActivityDestroy();
            }
        };

        ActivityComponent termuxTerminalSessionActivityClientComponent = new ActivityComponent() {
            @Override
            public void onActivityStop() {
                if (mTermuxTerminalSessionActivityClient != null)
                    mTermuxTerminalSessionActivityClient.onStop();
            }
        };

        ActivityComponent termuxTerminalViewClientComponent = new ActivityComponent() {
            @Override
            public void onActivityStop() {
                if (mTermuxTerminalViewClient != null)
                    mTermuxTerminalViewClient.onStop();
            }
        };

        ActivityComponent sessionSwitchPickerControllerComponent = new ActivityComponent() {
            @Override
            public void onActivityStop() {
                if (mSessionSwitchPickerController != null)
                    mSessionSwitchPickerController.onActivityStopped();
            }
        };

        mActivityComponents = Arrays.asList(
            sessionListBottomSheetComponent,
            rightDrawerComponent,
            browserComponent,
            termuxTerminalSessionActivityClientComponent,
            termuxTerminalViewClientComponent,
            sessionSwitchPickerControllerComponent);
    }

    private void checkForApkUpdateAndShowIndicator() {
        ApkUpdateForegroundCheckThrottle throttle = new ApkUpdateForegroundCheckThrottle(this);
        long nowMillis = System.currentTimeMillis();
        if (!throttle.shouldCheckNow(nowMillis)) {
            return;
        }
        throttle.recordCheckedAt(nowMillis);
        new ApkUpdateUiController(this).checkAndShowFloatingIndicator(new ApkUpdateFloatingIndicatorView());
    }

    private void requestPostNotificationsPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            REQUEST_POST_NOTIFICATIONS);
    }

    private final class ApkUpdateFloatingIndicatorView
        implements ApkUpdateFloatingIndicatorController.IndicatorView {

        @Override
        public void showUpdateAvailable(String latestVersionName, Runnable onTapped) {
            FloatingActionButton indicator = findViewById(R.id.apk_update_floating_indicator);
            if (indicator == null) return;
            indicator.setContentDescription(getString(
                R.string.apk_update_floating_indicator_content_description, latestVersionName));
            indicator.setOnClickListener(view -> onTapped.run());
            indicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void hide() {
            FloatingActionButton indicator = findViewById(R.id.apk_update_floating_indicator);
            if (indicator == null) return;
            indicator.setOnClickListener(null);
            indicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (mTermuxBrowserController != null)
            mTermuxBrowserController.reconfigureBrowserSplitForOrientation(isLandscape);

        if (mSessionListBottomSheetController != null)
            mSessionListBottomSheetController.applyOrientationBounds();
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;

        TermuxActivityHolder.set(this);

        if (mTermuxService != null)
            mTermuxService.onActivityForegrounded();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();

        if (mPreferences.isTerminalMarginAdjustmentEnabled())
            addTermuxActivityRootViewGlobalLayoutListener();

        registerTermuxActivityBroadcastReceiver();

        ApkUpdateUiController apkUpdateUiController = new ApkUpdateUiController(this);
        apkUpdateUiController.resumePendingInstallIfPermissionGranted();
        apkUpdateUiController.showPendingIndicatorIfAny(new ApkUpdateFloatingIndicatorView());

        checkForApkUpdateAndShowIndicator();
        requestPostNotificationsPermissionIfNeeded();

        mSessionDefinitionAutoReloadScheduler.onForeground(mPreferences.getSessionDefinitionReloadIntervalMinutes());
        mSessionReconnectScheduler.start(mPreferences.getBackgroundReconnectScanIntervalMinutes());
    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.logVerbose(LOG_TAG, "onResume");

        if (mIsInvalidState) return;

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onResume();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onResume();

        // Check if a crash happened on last run of the app or if a plugin crashed and show a
        // notification with the crash details if it did
        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, LOG_TAG);

        for (ActivityComponent component : mActivityComponents)
            component.onActivityResume();

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;

        mSessionDefinitionAutoReloadScheduler.onBackground();

        if (mTermuxService != null)
            mTermuxService.onActivityBackgrounded();

        removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();
        getDrawer().closeDrawers();

        for (ActivityComponent component : mActivityComponents)
            component.onActivityStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.logDebug(LOG_TAG, "onDestroy");

        if (mTtsManager != null)
            mTtsManager.shutdown();

        TermuxActivityHolder.clear(this);
        mWindowDrawTimeObserver.stop();
        ActivityWindowRecorderHolder.getInstance().recordActivityDestroyed();

        if (mIsInvalidState) return;

        mSessionReconnectScheduler.stop();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onDestroy();

        for (ActivityComponent component : mActivityComponents)
            component.onActivityDestroy();

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to activity.
            mTermuxService.setUpdateTagReasonTrigger(null);
            mTermuxService.setOpenTagUrlOpener(null);
            mTermuxService.setAppLauncher(null);
            mTermuxService.unsetTermuxTerminalSessionClient();
            mTermuxService = null;
        }

        try {
            unbindService(this);
        } catch (Exception e) {
            // ignore.
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
        saveTerminalToolbarTextInput(savedInstanceState);
        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);
    }





    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Logger.logDebug(LOG_TAG, "onServiceConnected");

        mTermuxService = ((TermuxService.LocalBinder) service).service;

        final Intent intent = getIntent();
        setIntent(null);

        mPendingExpandedProjectsAllowlist = parseExpandedProjectsAllowlist(intent);
        mPendingProjectActionTokens = parseProjectActionTokens(intent);
        mPendingInboundBrowserUrl = parseInboundBrowserUrl(intent);

        setTermuxSessionsListView();

        if (mTermuxService.isTermuxSessionsEmpty()) {
            if (mIsVisible) {
                TermuxInstaller.setupBootstrapIfNeeded(TermuxActivity.this, () -> {
                    if (mTermuxService == null) return; // Activity might have been destroyed.
                    try {
                        boolean launchFailsafe = false;
                        if (intent != null && intent.getExtras() != null) {
                            launchFailsafe = intent.getExtras().getBoolean(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                        }
                        createStartupSessions(launchFailsafe);
                        eagerLoadAllSessions();
                    } catch (WindowManager.BadTokenException e) {
                        // Activity finished - ignore.
                    }
                });
            } else {
                // The service connected while not in foreground - just bail out.
                finishActivityIfNotFinishing();
            }
        } else {
            mTermuxTerminalSessionActivityClient.syncPersistedSessionsWithLiveSessions();

            // If termux was started from launcher "New session" shortcut and activity is recreated,
            // then the original intent will be re-delivered, resulting in a new session being re-added
            // each time.
            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                boolean isFailSafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
                mTermuxTerminalSessionActivityClient.addNewSession(isFailSafe, null);
            } else {
                mTermuxTerminalSessionActivityClient.setCurrentSessionOnReconnectIfNoneDisplayed();
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);

        // Register the foreground update-flow trigger so a detected update tag (from any session)
        // can run the download and install-prompt while this activity is in the foreground.
        if (mUpdateTagUpdateRunner != null)
            mTermuxService.setUpdateTagReasonTrigger(mUpdateTagUpdateRunner);

        // Register the foreground URL opener so a detected open tag opens a tab while this activity is
        // in the foreground; the deduplication state stays in the service-owned controller.
        if (mOpenTagUrlOpener != null)
            mTermuxService.setOpenTagUrlOpener(mOpenTagUrlOpener);

        if (mAppLauncher != null)
            mTermuxService.setAppLauncher(mAppLauncher);

        eagerLoadAllSessions();

        routePendingInboundBrowserUrl();
    }

    private void createStartupSessions(boolean launchFailsafe) {
        if (launchFailsafe) {
            mTermuxTerminalSessionActivityClient.addNewSession(true, null);
            return;
        }

        boolean restoredPersistedSessions = mTermuxTerminalSessionActivityClient.restorePersistedSessions();
        boolean createdAlwaysPresentSessions = mTermuxTerminalSessionActivityClient.restoreAlwaysPresentSessions();
        restoreProjectManagerSessionsOnColdStart();

        if (restoredPersistedSessions || createdAlwaysPresentSessions) return;
        if (mTermuxService != null && !mTermuxService.isTermuxSessionsEmpty()) return;
        mTermuxTerminalSessionActivityClient.addNewSession(false, null);
    }

    private void restoreProjectManagerSessionsOnColdStart() {
        new SessionDefinitionController(this, mSessionDefinitionRepository, new SessionDefinitionPlanner())
            .restoreProjectManagerSessionsOnColdStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String inboundUrl = parseInboundBrowserUrl(intent);
        if (inboundUrl == null) return;
        mPendingInboundBrowserUrl = inboundUrl;
        routePendingInboundBrowserUrl();
    }

    @Nullable
    private String parseInboundBrowserUrl(@Nullable Intent intent) {
        if (intent == null) return null;
        return BrowserInboundViewUrl.resolveInAppBrowserUrl(intent.getAction(), intent.getDataString());
    }

    public void routePendingInboundBrowserUrl() {
        if (mPendingInboundBrowserUrl == null) return;
        if (mTermuxBrowserController == null) return;
        if (getCurrentSession() == null) return;
        String inboundUrl = mPendingInboundBrowserUrl;
        mPendingInboundBrowserUrl = null;
        mMainThreadHandler.post(() -> mTermuxBrowserController.openUrlInNewTab(inboundUrl));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logger.logDebug(LOG_TAG, "onServiceDisconnected");

        // Respect being stopped from the {@link TermuxService} notification action.
        finishActivityIfNotFinishing();
    }






    private void reloadProperties() {
        mProperties.loadTermuxPropertiesFromDisk();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadProperties();
    }



    private void setActivityTheme() {
        // Update NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        // Set activity night mode. If NightMode.SYSTEM is set, then android will automatically
        // trigger recreation of activity when uiMode/dark mode configuration is changed so that
        // day or night theme takes affect.
        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);
    }

    private void setMargins() {
        RelativeLayout relativeLayout = findViewById(R.id.activity_termux_root_relative_layout);
        int marginHorizontal = mProperties.getTerminalMarginHorizontal();
        int marginVertical = mProperties.getTerminalMarginVertical();
        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }



    public void addTermuxActivityRootViewGlobalLayoutListener() {
        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());
    }

    public void removeTermuxActivityRootViewGlobalLayoutListener() {
        if (getTermuxActivityRootView() != null)
            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());
    }



    private void setTermuxTerminalViewAndClients() {
        // Set termux terminal view and session clients
        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);
        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);

        // Set termux terminal view
        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);

        mOwnerCallDialogController = new OwnerCallDialogController(
            findViewById(R.id.activity_termux_root_relative_layout),
            mOwnerCallInbox::callsFor,
            this::currentOwnerCallDialogGeometry,
            new OwnerCallBodySpannedText.OwnerCallBodyTapActions() {
                @Override
                public void onCopyableTextTapped(@NonNull String text) {
                    ShareUtils.copyTextToClipboard(TermuxActivity.this, text,
                        getString(R.string.msg_owner_call_text_copied_to_clipboard));
                }

                @Override
                public void onUrlTapped(@NonNull String url) {
                    if (mTermuxTerminalViewClient != null) {
                        mTermuxTerminalViewClient.showUrlOpenChoice(url);
                    }
                }
            },
            new OwnerCallDialogController.OwnerCallDialogPlacementStore() {
                @Override
                @Nullable
                public OwnerCallDialogPlacement loadPlacement() {
                    return OwnerCallDialogStoredPlacement.of(getPreferences());
                }

                @Override
                public void savePlacement(@NonNull OwnerCallDialogPlacement placement) {
                    getPreferences().setOwnerCallDialogPlacement(placement.getLeftMarginPixels(),
                        placement.getBottomMarginPixels(), placement.getWidthPixels(),
                        placement.getHeightPixels());
                }
            });
        OwnerCallDialogRelayoutWatcher.watchTerminalArea(mTerminalView,
            this::renderUnansweredOwnerCallsOfDisplayedSession);

        View ownerCallPendingIndicator = findViewById(R.id.owner_call_pending_indicator);
        if (ownerCallPendingIndicator != null) {
            OwnerCallDialogController controller = mOwnerCallDialogController;
            ownerCallPendingIndicator.setOnClickListener(
                v -> controller.reopenDialog(System.currentTimeMillis()));
        }

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onCreate();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxSessionsListView() {
        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());
        mTermuxSessionListViewController.setCoalescedRefreshRunnable(this::applySessionListUpdate);
        loadSessionDefinitionEntriesForGrouping();
    }

    private void loadSessionDefinitionEntriesForGrouping() {
        if (mTermuxSessionListViewController == null) return;
        List<SessionDefinitionEntry> cachedEntries = mSessionDefinitionRepository.getCachedEntries();
        if (!cachedEntries.isEmpty()) {
            refreshDisplayedSessionDefinitionEntries(cachedEntries);
            return;
        }
        String baseUrl = getPreferences().getSessionDefinitionUrl().trim();
        mSessionDefinitionRepository.load(baseUrl, () -> {
            if (mTermuxSessionListViewController != null) {
                refreshDisplayedSessionDefinitionEntries(mSessionDefinitionRepository.getCachedEntries());
            }
        });
    }

    @NonNull
    private List<String> parseExpandedProjectsAllowlist(@Nullable Intent intent) {
        if (intent == null) {
            return Collections.emptyList();
        }
        List<String> allowlist = new ArrayList<>(
            ExpandedProjectsAllowlistParser.parse(intent.getDataString()));
        for (String token : ExpandedProjectsAllowlistParser.parse(intent.getStringExtra(EXTRA_EXPANDED_PROJECTS))) {
            if (!allowlist.contains(token)) {
                allowlist.add(token);
            }
        }
        return allowlist;
    }

    @NonNull
    private List<ProjectActionToken> parseProjectActionTokens(@Nullable Intent intent) {
        if (intent == null) {
            return Collections.emptyList();
        }
        List<ProjectActionToken> projectActionTokens = new ArrayList<>(
            ProjectActionTokenParser.parse(intent.getDataString()));
        for (ProjectActionToken token : ProjectActionTokenParser.parse(intent.getStringExtra(EXTRA_EXPANDED_PROJECTS))) {
            if (!projectActionTokens.contains(token)) {
                projectActionTokens.add(token);
            }
        }
        return projectActionTokens;
    }

    private void applyPendingExpandedProjectsAllowlist() {
        if (mTermuxSessionListViewController == null) {
            return;
        }
        if (!mPendingExpandedProjectsAllowlist.isEmpty()) {
            mTermuxSessionListViewController.applyExpandedProjectsAllowlist(mPendingExpandedProjectsAllowlist);
        }
        if (!mPendingProjectActionTokens.isEmpty()) {
            mTermuxSessionListViewController.applyProjectActionTokens(mPendingProjectActionTokens);
        }
    }



    private void setTerminalToolbarView(Bundle savedInstanceState) {
        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView,
            mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (mPreferences.shouldShowTerminalToolbar()) terminalToolbarViewPager.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        mTerminalToolbarDefaultHeight = layoutParams.height;

        setTerminalToolbarHeight();

        String savedTextInput = null;
        if (savedInstanceState != null)
            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);

        TerminalToolbarViewPager.PageAdapter pageAdapter = new TerminalToolbarViewPager.PageAdapter(this, savedTextInput);
        terminalToolbarViewPager.setAdapter(pageAdapter);

        View textInputRow = findViewById(R.id.terminal_toolbar_text_input_row);
        if (textInputRow != null) pageAdapter.setupTextInputRow(textInputRow);
    }

    private void setTerminalToolbarHeight() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();
        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *
            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *
            mProperties.getTerminalToolbarHeightScaleFactor());
        terminalToolbarViewPager.setLayoutParams(layoutParams);
    }

    public void toggleTerminalToolbar() {
        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();
        if (terminalToolbarViewPager == null) return;

        final boolean showNow = mPreferences.toogleShowTerminalToolbar();
        Logger.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);
        terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);
    }

    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);
        if (textInputView != null) {
            String textInput = textInputView.getText().toString();
            if (!textInput.isEmpty()) savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);
        }
    }



    public void openSettingsActivity() {
        ActivityUtils.startActivity(this, createSettingsActivityIntent(this));
    }

    static Intent createSettingsActivityIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    public void loadSessionsFromDefinition() {
        new SessionDefinitionController(this, mSessionDefinitionRepository, new SessionDefinitionPlanner()).loadAndBuildSessions(true);
    }

    public void reloadSessionsAndRefreshAllState() {
        loadSessionsFromDefinition();
        reconnectDeadDisplayedSessionsThenForceRescanStatusline();
    }

    public void reconnectDeadDisplayedSessions() {
        reconnectDeadDisplayedSessionsThenForceRescanStatusline();
    }

    public void reconnectDeadDefinitionBackedSessions() {
        reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline();
    }

    private void reconnectDeadDisplayedSessionsThenForceRescanStatusline() {
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.reconnectDeadDisplayedSessionsThenForceRescanStatusline();
    }

    private void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline();
    }

    public void prewarmSessionDefinitionDocument() {
        mSessionDefinitionPrewarm.prewarmSessionDefinitionDocument();
    }

    public void eagerLoadAllSessions() {
        if (mTerminalView == null || mTermuxService == null) return;

        if (!mTerminalView.isLaidOutForSizeComputation()) {
            mTerminalView.post(this::eagerLoadAllSessions);
            return;
        }

        mSessionEagerLoader.eagerLoadAllSessions();
    }

    private List<TerminalSession> collectSessionsToEagerLoad() {
        List<TerminalSession> sessions = new ArrayList<>();
        if (mTermuxService == null) return sessions;

        TermuxAppSharedPreferences preferences = getPreferences();
        if (preferences == null) return sessions;

        Set<String> hiddenSessionNames = preferences.getDisabledSessionNames();
        TerminalSession displayedSession = getCurrentSession();
        for (TermuxSession termuxSession : mTermuxService.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == displayedSession) continue;
            if (terminalSession != null && HiddenSessionNameMatcher.matchesAHiddenSession(
                    terminalSession.mSessionName, hiddenSessionNames)) continue;
            sessions.add(terminalSession);
        }
        return sessions;
    }

    private void eagerLoadSessionEmulator(@NonNull TerminalSession session) {
        if (session.getEmulator() != null) return;

        int[] dimensions = mTerminalView.computeSessionEmulatorDimensions();
        if (dimensions == null) return;

        session.updateSize(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
        mTermuxTerminalSessionActivityClient.termuxSessionListNotifyUpdated();
    }

    private void onSessionDefinitionDocumentPrewarmed() {
        refreshDisplayedSessionDefinitionEntries(mSessionDefinitionRepository.getCachedEntries());
    }

    public void refreshDisplayedSessionDefinitionEntries(@NonNull List<SessionDefinitionEntry> entries) {
        if (mTermuxSessionListViewController != null) {
            mTermuxSessionListViewController.setEntries(entries);
            applyPendingExpandedProjectsAllowlist();
        }
        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.reapplyStartupDisplayedSessionAfterEntriesLoaded(!entries.isEmpty());
        showUnansweredOwnerCallsOfDisplayedSession();
    }

    public void showUnansweredOwnerCallsOfDisplayedSession() {
        if (mOwnerCallDialogController == null) return;

        String sessionName = displayedSessionName();
        mOwnerCallInbox.refreshFor(sessionName, sessionIsCallingTheOwner(sessionName),
            ownerCallFileUrlForSession(sessionName), System.currentTimeMillis(),
            this::renderUnansweredOwnerCallsOfDisplayedSession);
        renderUnansweredOwnerCallsOfDisplayedSession();
    }

    public void deleteAnsweredOwnerCallsOfSession(@Nullable String sessionName) {
        mOwnerCallInbox.deleteAnsweredCalls(sessionName, ownerCallFileUrlForSession(sessionName),
            this::renderUnansweredOwnerCallsOfDisplayedSession);
    }

    private void renderUnansweredOwnerCallsOfDisplayedSession() {
        if (mOwnerCallDialogController == null) return;

        String sessionName = displayedSessionName();
        mOwnerCallDialogController.showCallsForSession(sessionName, System.currentTimeMillis());
        updateOwnerCallPendingIndicator(sessionName);
    }

    private void updateOwnerCallPendingIndicator(@Nullable String sessionName) {
        View indicator = findViewById(R.id.owner_call_pending_indicator);
        if (indicator == null) return;
        boolean hasCalls = !mOwnerCallInbox.callsFor(sessionName).isEmpty();
        int newVisibility = hasCalls ? View.VISIBLE : View.GONE;
        if (indicator.getVisibility() != newVisibility) {
            indicator.setVisibility(newVisibility);
        }
    }

    @Nullable
    private String displayedSessionName() {
        TerminalSession session = getCurrentSession();
        return session == null ? null : session.mSessionName;
    }

    private boolean sessionIsCallingTheOwner(@Nullable String sessionName) {
        SessionNewActivityStore store = getSessionNewActivityStore();
        return sessionName != null && store != null && store.hasPendingExplicitCall(sessionName);
    }

    @Nullable
    private String ownerCallFileUrlForSession(@Nullable String sessionName) {
        return OwnerCallFileUrl.resolve(getPreferences().getSessionDefinitionUrl(),
            new SessionDefinitionEntryMatcher().findGroupLabelForSessionName(
                mSessionDefinitionRepository.getCachedEntries(), sessionName),
            sessionName);
    }

    @NonNull
    private OwnerCallDialogGeometry currentOwnerCallDialogGeometry() {
        return OwnerCallDialogViewport.resolve(findViewById(R.id.activity_termux_root_relative_layout),
            mTerminalView, getResources().getDisplayMetrics().heightPixels,
            mTerminalView.mRenderer == null ? 0 : mTerminalView.mRenderer.getFontLineSpacing());
    }

    public void promptAndCreateNewSession() {
        TextInputDialogUtils.textInput(TermuxActivity.this,
            R.string.action_new_session, null,
            R.string.action_create_named_session_confirm,
            text -> {
                String trimmedName = text.trim();
                String sessionName = trimmedName.isEmpty() ? null : trimmedName;
                mTermuxTerminalSessionActivityClient.addNewSessionApplyingAutosshConfig(sessionName);
            },
            -1, null, -1, null, null);
    }

    private void setBrowserView() {
        mTermuxBrowserController = new TermuxBrowserController(this);
        mOpenTagUrlOpener = new OpenTagUrlNativeAppOpener(this,
            mTermuxBrowserController::openUrlInTabForSession);
        mAppLauncher = new InstalledAppLauncher(
            getPackageManager()::getLaunchIntentForPackage, this::startActivity);
        // The update-flow trigger only needs the activity for its UI; it is registered with the
        // service-owned controller in onServiceConnected once the service is bound.
        mUpdateTagUpdateRunner = new UpdateTagUpdateRunner(this, new ApkUpdateFloatingIndicatorView());
    }

    private void setBrowserToggleBarView() {
        findViewById(R.id.terminal_toolbar_browser_toggle_button).setOnClickListener(v -> {
            getTermuxBrowserController().toggleBrowser();
        });
    }

    private void setKeyboardToggleBarView() {
        findViewById(R.id.terminal_toolbar_keyboard_toggle_button).setOnClickListener(v -> {
            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
        });
    }

    private void setRightDrawerToggleBarView() {
        findViewById(R.id.terminal_toolbar_right_drawer_toggle_button).setOnClickListener(v -> {
            getTermuxBrowserController().toggleTabsDrawer();
        });
    }

    private void setSessionSheetToggleBarView() {
        mSessionListBottomSheetController = new SessionListBottomSheetController(this);
        findViewById(R.id.terminal_toolbar_session_sheet_button).setOnClickListener(v -> {
            mSessionListBottomSheetController.toggle();
        });
    }

    private void setSessionNavigationButtonsView() {
        mPreviousSessionButton = findViewById(R.id.terminal_toolbar_previous_session_button);
        mNextSessionButton = findViewById(R.id.terminal_toolbar_next_session_button);
        mPreviousSessionCountBadge = findViewById(R.id.terminal_toolbar_previous_session_count_badge);
        mNextSessionCountBadge = findViewById(R.id.terminal_toolbar_next_session_count_badge);
        mJumpToCallingSessionButton = findViewById(R.id.terminal_toolbar_jump_to_calling_session_button);
        mJumpToCallingSessionCountBadge =
            findViewById(R.id.terminal_toolbar_jump_to_calling_session_count_badge);
        SessionNavigationButtonsBinder.bind(
            mPreviousSessionButton,
            mNextSessionButton,
            forward -> getSessionSwitchPickerController().onVolumeKeyDirection(forward));
        mJumpToCallingSessionButton.setOnClickListener(v -> jumpToTopmostCallingSession());
    }

    private void jumpToTopmostCallingSession() {
        if (mTermuxSessionListViewController == null) {
            return;
        }
        int topmostCallingSessionIndex = CallingSessionNavigator.topmostCallingSessionIndex(
            mTermuxSessionListViewController.getOrderedSessionIndexes(),
            mTermuxSessionListViewController.getSessionNamesByIndex(),
            mTermuxSessionListViewController.getPendingCallToUserSessionNames());
        if (topmostCallingSessionIndex < 0) {
            return;
        }
        mTermuxSessionListViewController.switchToSessionAtIndex(topmostCallingSessionIndex);
        if (mSessionListBottomSheetController != null) {
            mSessionListBottomSheetController.hide();
        }
    }

    private void renderSessionNavigationActivityTier() {
        if (mPreviousSessionButton == null || mNextSessionButton == null) {
            return;
        }
        CallingSessionSplit callingSessionSplit = computeCallingSessionSplit();
        SessionActivityDirection direction = SessionActivityDirection.ofCallingSessionSplit(callingSessionSplit);
        int redColor = ContextCompat.getColor(this, R.color.session_activity_tier_red);
        int defaultColor = ContextCompat.getColor(this, com.termux.shared.R.color.white);
        SessionNavigationButtonsBinder.applyDirectionTier(
            mPreviousSessionButton, mNextSessionButton, direction, redColor, defaultColor);
        if (mPreviousSessionCountBadge != null && mNextSessionCountBadge != null) {
            SessionNavigationButtonsBinder.applyDirectionCountBadges(
                mPreviousSessionCountBadge, mNextSessionCountBadge, direction);
        }
        renderJumpToCallingSessionButton(callingSessionSplit, redColor, defaultColor);
    }

    private void renderJumpToCallingSessionButton(@NonNull CallingSessionSplit callingSessionSplit,
                                                  int redColor, int defaultColor) {
        if (mJumpToCallingSessionButton == null || mJumpToCallingSessionCountBadge == null) {
            return;
        }
        int callingSessionCount = callingSessionSplit.getTotalCount();
        mJumpToCallingSessionButton.setColorFilter(
            callingSessionCount >= 1 ? redColor : defaultColor);
        SessionNavigationButtonsBinder.applyCallingSessionCountBadge(
            mJumpToCallingSessionCountBadge, callingSessionCount);
    }

    @NonNull
    private CallingSessionSplit computeCallingSessionSplit() {
        TermuxService service = getTermuxService();
        if (service == null || mTermuxSessionListViewController == null) {
            return CallingSessionNavigator.split(
                Collections.emptyList(), Collections.emptyList(), Collections.emptySet(), -1);
        }
        List<Integer> orderedSessionIndexes = mTermuxSessionListViewController.getOrderedSessionIndexes();
        List<String> sessionNamesByIndex = mTermuxSessionListViewController.getSessionNamesByIndex();
        Set<String> callingSessionNames = mTermuxSessionListViewController.getPendingCallToUserSessionNames();
        int currentSessionIndex = service.getIndexOfSession(getCurrentSession());
        return CallingSessionNavigator.split(
            orderedSessionIndexes, sessionNamesByIndex, callingSessionNames, currentSessionIndex);
    }





    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        for (ActivityComponent component : mActivityComponents) {
            if (component.onBackPressed()) {
                return;
            }
        }
        finishActivityIfNotFinishing();
    }

    public void finishActivityIfNotFinishing() {
        // prevent duplicate calls to finish() if called from multiple places
        if (!TermuxActivity.this.isFinishing()) {
            finish();
        }
    }

    /** Show a toast and dismiss the last one if still visible. */
    public void showToast(String text, boolean longDuration) {
        if (text == null || text.isEmpty()) return;
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(TermuxActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentSession();
        if (currentSession == null) return;

        boolean autoFillEnabled = mTerminalView.isAutoFillEnabled();

        if (mTermuxTerminalViewClient != null) {
            for (LongPressedUrlMenuItem item : longPressedUrlMenuItems(this, mTermuxTerminalViewClient.getLongPressedUrl())) {
                menu.add(Menu.NONE, item.getMenuItemId(), Menu.NONE, item.getTitle());
            }
        }

        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);
        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);
        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText())) {
            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);
            menu.add(Menu.NONE, CONTEXT_MENU_TRANSLATE_SELECTED_TEXT, Menu.NONE, R.string.action_translate_selected_text);
            menu.add(Menu.NONE, CONTEXT_MENU_SEND_SELECTED_TEXT_TO_TERMINAL, Menu.NONE, R.string.action_send_selected_text_to_terminal);
        }
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);
        if (autoFillEnabled)
            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);
    }

    /** Hook system menu to show context menu instead. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mTerminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentSession();

        switch (item.getItemId()) {
            case CONTEXT_MENU_OPEN_LINK_IN_BROWSER_ID:
                mTermuxTerminalViewClient.openLongPressedUrlInApp();
                return true;
            case CONTEXT_MENU_OPEN_LINK_IN_BROWSER_BACKGROUND_ID:
                mTermuxTerminalViewClient.openLongPressedUrlInAppBackground();
                return true;
            case CONTEXT_MENU_OPEN_LINK_IN_CHROME_ID:
                mTermuxTerminalViewClient.openLongPressedUrlInChrome();
                return true;
            case CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID:
                mTermuxTerminalViewClient.openLongPressedUrlInNativeApp();
                return true;
            case CONTEXT_MENU_COPY_LINK_URL_ID:
                mTermuxTerminalViewClient.copyLongPressedUrlToClipboard();
                return true;
            case CONTEXT_MENU_SELECT_URL_ID:
                mTermuxTerminalViewClient.showUrlSelection();
                return true;
            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:
                mTermuxTerminalViewClient.shareSessionTranscript();
                return true;
            case CONTEXT_MENU_SHARE_SELECTED_TEXT:
                mTermuxTerminalViewClient.shareSelectedText();
                return true;
            case CONTEXT_MENU_TRANSLATE_SELECTED_TEXT:
                translateSelectedText();
                return true;
            case CONTEXT_MENU_SEND_SELECTED_TEXT_TO_TERMINAL:
                sendSelectedTextToTerminal(session);
                return true;
            case CONTEXT_MENU_AUTOFILL_USERNAME:
                mTerminalView.requestAutoFillUsername();
                return true;
            case CONTEXT_MENU_AUTOFILL_PASSWORD:
                mTerminalView.requestAutoFillPassword();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason
        mTerminalView.onContextMenuClosed(menu);
        if (mTermuxTerminalViewClient != null) {
            mTermuxTerminalViewClient.clearLongPressedUrl();
        }
    }

    private void translateSelectedText() {
        String selectedText = mTerminalView.getStoredSelectedText();
        if (DataUtils.isNullOrEmpty(selectedText)) return;

        if (startGoogleTranslate(selectedText)) return;

        Intent processTextIntent = createProcessTextIntent(selectedText);
        if (processTextIntent.resolveActivity(getPackageManager()) == null) {
            showToast(getString(R.string.msg_no_translation_app_found), true);
            return;
        }

        try {
            startActivity(Intent.createChooser(processTextIntent, getString(R.string.title_translate_selected_text_with)));
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.msg_no_translation_app_found), true);
        }
    }

    private boolean startGoogleTranslate(String selectedText) {
        Intent googleTranslateIntent = createGoogleTranslateProcessTextIntent(selectedText);
        if (googleTranslateIntent.resolveActivity(getPackageManager()) == null) return false;

        try {
            startActivity(googleTranslateIntent);
            return true;
        } catch (ActivityNotFoundException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to launch Google Translate directly for selected text translation", e);
            return false;
        }
    }

    static Intent createProcessTextIntent(String selectedText) {
        Intent processTextIntent = new Intent(Intent.ACTION_PROCESS_TEXT);
        processTextIntent.setType("text/plain");
        processTextIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText);
        processTextIntent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
        return processTextIntent;
    }

    static Intent createGoogleTranslateProcessTextIntent(String selectedText) {
        Intent googleTranslateIntent = createProcessTextIntent(selectedText);
        googleTranslateIntent.setPackage(GOOGLE_TRANSLATE_PACKAGE_NAME);
        return googleTranslateIntent;
    }

    static String longPressedUrlNativeAppMenuTitle(Context context, String longPressedUrl) {
        NativeAppLink.NativeAppTarget nativeAppTarget = NativeAppLink.resolveTarget(longPressedUrl);
        if (nativeAppTarget == null) return null;
        return context.getString(R.string.action_open_link_in_native_app,
            nativeAppTarget.getAppDisplayName());
    }

    static final class LongPressedUrlMenuItem {

        private final int mMenuItemId;

        private final CharSequence mTitle;

        LongPressedUrlMenuItem(int menuItemId, CharSequence title) {
            this.mMenuItemId = menuItemId;
            this.mTitle = title;
        }

        int getMenuItemId() {
            return mMenuItemId;
        }

        CharSequence getTitle() {
            return mTitle;
        }
    }

    static List<LongPressedUrlMenuItem> longPressedUrlMenuItems(Context context, String longPressedUrl) {
        List<LongPressedUrlMenuItem> items = new ArrayList<>();
        if (!TermuxTerminalViewClient.shouldShowLongPressedUrlMenuItems(longPressedUrl)) return items;
        items.add(new LongPressedUrlMenuItem(CONTEXT_MENU_OPEN_LINK_IN_BROWSER_ID,
            context.getString(R.string.action_open_link_in_browser)));
        items.add(new LongPressedUrlMenuItem(CONTEXT_MENU_OPEN_LINK_IN_BROWSER_BACKGROUND_ID,
            context.getString(R.string.action_open_link_in_browser_background)));
        items.add(new LongPressedUrlMenuItem(CONTEXT_MENU_OPEN_LINK_IN_CHROME_ID,
            context.getString(R.string.action_open_link_in_chrome)));
        String nativeAppMenuTitle = longPressedUrlNativeAppMenuTitle(context, longPressedUrl);
        if (nativeAppMenuTitle != null) {
            items.add(new LongPressedUrlMenuItem(CONTEXT_MENU_OPEN_LINK_IN_GOOGLE_APP_ID, nativeAppMenuTitle));
        }
        items.add(new LongPressedUrlMenuItem(CONTEXT_MENU_COPY_LINK_URL_ID,
            context.getString(R.string.action_copy_link_url)));
        return items;
    }

    private void sendSelectedTextToTerminal(TerminalSession session) {
        if (session == null) return;
        if (!session.isRunning()) return;
        String selectedText = mTerminalView.getStoredSelectedText();
        if (DataUtils.isNullOrEmpty(selectedText)) return;
        session.getEmulator().paste(selectedText);
    }

    /**
     * For processes to access primary external storage (/sdcard, /storage/emulated/0, ~/storage/shared),
     * termux needs to be granted legacy WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permissions
     * if targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) and higher.
     */
    public void requestStoragePermission(boolean isPermissionCallback) {
        new Thread() {
            @Override
            public void run() {
                // Do not ask for permission again
                int requestCode = isPermissionCallback ? -1 : PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION;

                // If permission is granted, then also setup storage symlinks.
                if(PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(
                    TermuxActivity.this, requestCode, !isPermissionCallback)) {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_granted_on_request));

                    TermuxInstaller.setupStorageSymlinks(TermuxActivity.this);
                } else {
                    if (isPermissionCallback)
                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,
                            getString(com.termux.shared.R.string.msg_storage_permission_not_granted_on_request));
                }
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: "  + resultCode + ", data: "  + IntentUtils.getIntentString(data));
        if (requestCode == TermuxBrowserController.REQUEST_BROWSER_FILE_CHOOSER) {
            if (mTermuxBrowserController != null) {
                mTermuxBrowserController.deliverFileChooserResult(resultCode, data);
            }
            return;
        }
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.logVerbose(LOG_TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", permissions: "  + Arrays.toString(permissions) + ", grantResults: "  + Arrays.toString(grantResults));
        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {
            requestStoragePermission(true);
            return;
        }
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            Logger.logInfo(LOG_TAG, "POST_NOTIFICATIONS permission result: "
                + (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ? "granted" : "not granted"));
            return;
        }
        if (mTermuxBrowserController != null
                && mTermuxBrowserController.deliverMediaCapturePermissionResult(requestCode, permissions, grantResults)) {
            return;
        }
    }



    public int getNavBarHeight() {
        return mNavBarHeight;
    }

    public TermuxActivityRootView getTermuxActivityRootView() {
        return mTermuxActivityRootView;
    }

    public View getTermuxActivityBottomSpaceView() {
        return mTermuxActivityBottomSpaceView;
    }

    public ProgressBar getSessionDefinitionLoadingProgressBar() {
        return findViewById(R.id.session_definition_loading_progress_bar);
    }

    public ExtraKeysView getExtraKeysView() {
        return mExtraKeysView;
    }

    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {
        return mTermuxTerminalExtraKeys;
    }

    public void setExtraKeysView(ExtraKeysView extraKeysView) {
        mExtraKeysView = extraKeysView;
    }

    public DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    public SessionListBottomSheetController getSessionListBottomSheetController() {
        return mSessionListBottomSheetController;
    }

    public SessionSwitchPickerController getSessionSwitchPickerController() {
        return mSessionSwitchPickerController;
    }

    @Nullable
    public SessionNewActivityStore getSessionNewActivityStore() {
        return sessionNewActivityStoreOrNull(mTermuxService);
    }

    @Nullable
    static SessionNewActivityStore sessionNewActivityStoreOrNull(@Nullable TermuxService termuxService) {
        if (termuxService == null) {
            return null;
        }
        return termuxService.getSessionNewActivityStore();
    }

    public TermuxBrowserController getTermuxBrowserController() {
        return mTermuxBrowserController;
    }

    @Nullable
    public AppOpenTagController getAppOpenTagController() {
        return mTermuxService == null ? null : mTermuxService.getAppOpenTagController();
    }

    @Nullable
    public OpenTagBrowserController getOpenTagBrowserController() {
        return mTermuxService == null ? null : mTermuxService.getOpenTagBrowserController();
    }

    public UpdateTagUpdateController getUpdateTagUpdateController() {
        return mTermuxService == null ? null : mTermuxService.getUpdateTagUpdateController();
    }

    public CallToUserTagController getCallToUserTagController() {
        return mTermuxService == null ? null : mTermuxService.getCallToUserTagController();
    }


    public ViewPager getTerminalToolbarViewPager() {
        return (ViewPager) findViewById(R.id.terminal_toolbar_view_pager);
    }

    @Nullable
    public EditText getTerminalToolbarTextInput() {
        return findViewById(R.id.terminal_toolbar_text_input);
    }

    @Nullable
    public TerminalToolbarViewPager.PageAdapter getTerminalToolbarViewPagerAdapter() {
        ViewPager viewPager = getTerminalToolbarViewPager();
        if (viewPager == null) return null;
        return (TerminalToolbarViewPager.PageAdapter) viewPager.getAdapter();
    }

    public float getTerminalToolbarDefaultHeight() {
        return mTerminalToolbarDefaultHeight;
    }

    public boolean isTerminalViewSelected() {
        EditText textInput = getTerminalToolbarTextInput();
        return textInput == null || !textInput.hasFocus();
    }

    public boolean isTerminalToolbarTextInputViewSelected() {
        return getTerminalToolbarTextInput() != null;
    }


    public void termuxSessionListNotifyUpdated() {
        if (mTermuxSessionListViewController == null) return;
        mTermuxSessionListViewController.requestSessionListRefresh();
    }

    private void applySessionListUpdate() {
        if (mTermuxSessionListViewController == null) return;
        mTermuxSessionListViewController.refreshSessionList();
        renderSessionNavigationActivityTier();
        if (mSessionListBottomSheetController != null) {
            mSessionListBottomSheetController.refreshSessionCountTitleIfShowing();
        }
        showUnansweredOwnerCallsOfDisplayedSession();
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public boolean isOnResumeAfterOnCreate() {
        return mIsOnResumeAfterOnCreate;
    }

    public boolean isActivityRecreated() {
        return mIsActivityRecreated;
    }



    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mTermuxTerminalViewClient != null
            && mTermuxTerminalViewClient.handleVolumeKeysSwitchSessions(
                event.getKeyCode(), event.getAction() == KeyEvent.ACTION_DOWN)) {
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN
            && mSessionSwitchPickerController != null
            && mSessionSwitchPickerController.isShowing()) {
            mSessionSwitchPickerController.commitAndHide();
        }
        return super.dispatchKeyEvent(event);
    }

    public TermuxService getTermuxService() {
        return mTermuxService;
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxTerminalViewClient getTermuxTerminalViewClient() {
        return mTermuxTerminalViewClient;
    }

    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {
        return mTermuxTerminalSessionActivityClient;
    }

    public TtsManager getTtsManager() {
        return mTtsManager;
    }

    public TermuxSessionsListViewController getTermuxSessionListViewController() {
        return mTermuxSessionListViewController;
    }

    @NonNull
    public List<SessionDefinitionEntry> getSessionDefinitionEntries() {
        return Collections.unmodifiableList(mSessionDefinitionRepository.getCachedEntries());
    }

    @Nullable
    public TerminalSession getCurrentSession() {
        if (mTerminalView != null)
            return mTerminalView.getCurrentSession();
        else
            return null;
    }

    public TermuxAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxAppSharedProperties getProperties() {
        return mProperties;
    }




    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {
        // Make sure that terminal styling is always applied.
        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);
        context.sendBroadcast(stylingIntent);
    }

    private void registerTermuxActivityBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);
        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);
    }

    private void unregisterTermuxActivityBroadcastReceiver() {
        unregisterReceiver(mTermuxActivityBroadcastReceiver);
    }

    private void fixTermuxActivityBroadcastReceiverIntent(Intent intent) {
        if (intent == null) return;

        String extraReloadStyle = intent.getStringExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
        if ("storage".equals(extraReloadStyle)) {
            intent.removeExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);
            intent.setAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);
        }
    }

    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (mIsVisible) {
                fixTermuxActivityBroadcastReceiverIntent(intent);

                switch (intent.getAction()) {
                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:
                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");
                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);
                        return;
                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:
                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");
                        reloadActivityStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));
                        return;
                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:
                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");
                        requestStoragePermission(false);
                        return;
                    default:
                }
            }
        }
    }

    private void reloadActivityStyling(boolean recreateActivity) {
        if (mProperties != null) {
            reloadProperties();

            if (mExtraKeysView != null) {
                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());
                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);
            }

            // Update NightMode.APP_NIGHT_MODE
            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());
        }

        setMargins();
        setTerminalToolbarHeight();

        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onReloadActivityStyling();

        // To change the activity and drawer theme, activity needs to be recreated.
        // It will destroy the activity, including all stored variables and views, and onCreate()
        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.
        if (recreateActivity) {
            Logger.logDebug(LOG_TAG, "Recreating activity");
            TermuxActivity.this.recreate();
        }
    }



    public static void startTermuxActivity(@NonNull final Context context) {
        ActivityUtils.startActivity(context, newInstance(context));
    }

    public static Intent newInstance(@NonNull final Context context) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

}
