package com.termux.app;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.termux.R;
import com.termux.app.api.file.FileReceiverActivity;
import com.termux.app.apkupdate.ApkUpdateAutoCheckThrottle;
import com.termux.app.apkupdate.ApkUpdateManager;
import com.termux.app.apkupdate.ApkUpdateUiController;
import com.termux.app.terminal.TermuxActivityRootView;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.tts.TtsManager;
import com.termux.app.browser.OpenTagBrowserController;
import com.termux.app.browser.ProjectBrowserOverlayController;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.terminal.io.TermuxTerminalExtraKeys;
import com.termux.shared.activities.ReportActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.android.PermissionUtils;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.termux.app.sessiondefinition.HttpSessionDefinitionDocumentFetcher;
import com.termux.app.sessiondefinition.SessionDefinitionController;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionLoader;
import com.termux.app.sessiondefinition.SessionDefinitionParser;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.app.terminal.ExpandedProjectsAllowlistParser;
import com.termux.app.terminal.ProjectActionToken;
import com.termux.app.terminal.ProjectActionTokenParser;
import com.termux.app.terminal.SessionDefinitionEntriesProvider;
import com.termux.app.terminal.SessionNewActivityStore;
import com.termux.app.terminal.SessionListBottomSheetController;
import com.termux.app.terminal.SessionNavigationButtonsBinder;
import com.termux.app.terminal.SessionSwitchPickerController;
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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    SessionListBottomSheetController mSessionListBottomSheetController;

    SessionSwitchPickerController mSessionSwitchPickerController;

    private final SessionDefinitionEntriesProvider mSessionDefinitionEntriesProvider =
        new SessionDefinitionEntriesProvider(new SessionDefinitionLoader(
            new HttpSessionDefinitionDocumentFetcher(), new SessionDefinitionParser()));

    /**
     * The in-app browser controller managing the {@link android.webkit.WebView} and per-session tabs.
     */
    TermuxBrowserController mTermuxBrowserController;

    /**
     * The full-screen, project-scoped browser surface for project-level URLs, kept separate from per-session tabs.
     */
    ProjectBrowserOverlayController mProjectBrowserOverlayController;

    /**
     * Opens an `http`/`https` URL inside a `<open>...</open>` tag in the terminal output in the in-app browser.
     */
    OpenTagBrowserController mOpenTagBrowserController;

    /**
     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.
     */
    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();

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


    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;
    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;
    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;
    private static final int CONTEXT_MENU_SEND_SELECTED_TEXT_TO_TERMINAL = 12;
    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 11;
    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 2;
    private static final int CONTEXT_MENU_TRANSLATE_SELECTED_TEXT = 13;
    private static final int CONTEXT_MENU_OPEN_LINK_IN_BROWSER_ID = 14;
    private static final int CONTEXT_MENU_OPEN_LINK_IN_CHROME_ID = 15;

    static final String GOOGLE_TRANSLATE_PACKAGE_NAME = "com.google.android.apps.translate";

    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";
    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";

    private static final String LOG_TAG = "TermuxActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.logDebug(LOG_TAG, "onCreate");
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

        setStopSpeakingButtonView();

        setSessionSheetToggleBarView();

        mSessionSwitchPickerController = new SessionSwitchPickerController(this);

        setSessionNavigationButtonsView();

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

        maybeAutoCheckForApkUpdate();
    }

    private void maybeAutoCheckForApkUpdate() {
        if (!ApkUpdateManager.isAutoCheckEnabled(this)) {
            return;
        }
        ApkUpdateAutoCheckThrottle throttle = new ApkUpdateAutoCheckThrottle(this);
        long nowMillis = System.currentTimeMillis();
        if (!throttle.shouldCheckNow(nowMillis)) {
            return;
        }
        throttle.recordCheckedAt(nowMillis);
        new ApkUpdateUiController(this).checkAndPrompt(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.logDebug(LOG_TAG, "onStart");

        if (mIsInvalidState) return;

        mIsVisible = true;

        if (mTermuxService != null)
            mTermuxService.onActivityForegrounded();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStart();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStart();

        if (mPreferences.isTerminalMarginAdjustmentEnabled())
            addTermuxActivityRootViewGlobalLayoutListener();

        registerTermuxActivityBroadcastReceiver();
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

        mIsOnResumeAfterOnCreate = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        Logger.logDebug(LOG_TAG, "onStop");

        if (mIsInvalidState) return;

        mIsVisible = false;

        if (mTermuxService != null)
            mTermuxService.onActivityBackgrounded();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onStop();

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onStop();

        removeTermuxActivityRootViewGlobalLayoutListener();

        unregisterTermuxActivityBroadcastReceiver();
        getDrawer().closeDrawers();

        if (mTermuxBrowserController != null)
            mTermuxBrowserController.onActivityStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.logDebug(LOG_TAG, "onDestroy");

        if (mTtsManager != null)
            mTtsManager.shutdown();

        if (mIsInvalidState) return;

        if (mTermuxBrowserController != null)
            mTermuxBrowserController.onActivityDestroy();

        if (mProjectBrowserOverlayController != null)
            mProjectBrowserOverlayController.onActivityDestroy();

        if (mTermuxService != null) {
            // Do not leave service and session clients with references to activity.
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
                        if (launchFailsafe
                                || (!mTermuxTerminalSessionActivityClient.restorePersistedSessions()
                                    && !mTermuxTerminalSessionActivityClient.restoreAlwaysPresentSessions())) {
                            mTermuxTerminalSessionActivityClient.addNewSession(launchFailsafe, null);
                        }
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
                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());
            }
        }

        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.
        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);
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

        if (mTermuxTerminalViewClient != null)
            mTermuxTerminalViewClient.onCreate();

        if (mTermuxTerminalSessionActivityClient != null)
            mTermuxTerminalSessionActivityClient.onCreate();
    }

    private void setTermuxSessionsListView() {
        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());
        loadSessionDefinitionEntriesForGrouping();
    }

    private void loadSessionDefinitionEntriesForGrouping() {
        if (mTermuxSessionListViewController == null) return;
        List<SessionDefinitionEntry> cachedEntries = mSessionDefinitionEntriesProvider.getEntries();
        if (!cachedEntries.isEmpty()) {
            mTermuxSessionListViewController.setEntries(cachedEntries);
            applyPendingExpandedProjectsAllowlist();
            return;
        }
        String baseUrl = getPreferences().getSessionDefinitionUrl().trim();
        mSessionDefinitionEntriesProvider.load(baseUrl, () -> {
            if (mTermuxSessionListViewController != null) {
                mTermuxSessionListViewController.setEntries(mSessionDefinitionEntriesProvider.getEntries());
                applyPendingExpandedProjectsAllowlist();
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
        new SessionDefinitionController(this).loadAndBuildSessions();
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
        mProjectBrowserOverlayController = new ProjectBrowserOverlayController(this);
        mOpenTagBrowserController = new OpenTagBrowserController(mPreferences, mTermuxBrowserController::openUrlInNewTab);
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

    private void setStopSpeakingButtonView() {
        findViewById(R.id.terminal_toolbar_stop_speaking_button).setOnClickListener(v -> {
            if (mTtsManager != null) mTtsManager.stop();
            Logger.showToast(this, getString(R.string.msg_stopped_speaking), true);
        });
    }

    private void setSessionSheetToggleBarView() {
        mSessionListBottomSheetController = new SessionListBottomSheetController(this);
        findViewById(R.id.terminal_toolbar_session_sheet_button).setOnClickListener(v -> {
            mSessionListBottomSheetController.toggle();
        });
    }

    private void setSessionNavigationButtonsView() {
        SessionNavigationButtonsBinder.bind(
            findViewById(R.id.terminal_toolbar_previous_session_button),
            findViewById(R.id.terminal_toolbar_next_session_button),
            forward -> getSessionSwitchPickerController().onVolumeKeyDirection(forward));
    }





    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        if (mProjectBrowserOverlayController != null && mProjectBrowserOverlayController.onBackPressed()) {
            // Project browser handled the back press (navigated back or closed the overlay).
        } else if (mSessionListBottomSheetController != null && mSessionListBottomSheetController.isOpen()) {
            mSessionListBottomSheetController.hide();
        } else if (getDrawer().isDrawerOpen(Gravity.RIGHT)) {
            getDrawer().closeDrawers();
        } else if (mTermuxBrowserController != null && mTermuxBrowserController.onBackPressed()) {
            // Browser handled the back press (navigated back or returned to terminal).
        } else {
            finishActivityIfNotFinishing();
        }
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

        if (mTermuxTerminalViewClient != null && !DataUtils.isNullOrEmpty(mTermuxTerminalViewClient.getLongPressedUrl())) {
            menu.add(Menu.NONE, CONTEXT_MENU_OPEN_LINK_IN_BROWSER_ID, Menu.NONE, R.string.action_open_link_in_browser);
            menu.add(Menu.NONE, CONTEXT_MENU_OPEN_LINK_IN_CHROME_ID, Menu.NONE, R.string.action_open_link_in_chrome);
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
            case CONTEXT_MENU_OPEN_LINK_IN_CHROME_ID:
                mTermuxTerminalViewClient.openLongPressedUrlInChrome();
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

    public SessionNewActivityStore getSessionNewActivityStore() {
        return mTermuxService.getSessionNewActivityStore();
    }

    public TermuxBrowserController getTermuxBrowserController() {
        return mTermuxBrowserController;
    }

    public ProjectBrowserOverlayController getProjectBrowserOverlayController() {
        return mProjectBrowserOverlayController;
    }

    public OpenTagBrowserController getOpenTagBrowserController() {
        return mOpenTagBrowserController;
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
        mTermuxSessionListViewController.notifyDataSetChanged();
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
        return Collections.unmodifiableList(mSessionDefinitionEntriesProvider.getEntries());
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
