package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.BrowserSessionRemovalReason;
import com.termux.app.appopen.AppOpenTagController;
import com.termux.app.browser.OpenTagBrowserController;
import com.termux.app.browser.SessionNameBrowserTabUrlResolver;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.diagnostics.BackgroundOutputScanCostCounterHolder;
import com.termux.app.diagnostics.ForegroundOpenTagScanCostCounterHolder;
import com.termux.app.diagnostics.DiagnosticEventLogHolder;
import com.termux.app.diagnostics.DiagnosticEventType;
import com.termux.app.sessiondefinition.DeadSessionReconnectPlanner;
import com.termux.app.sessiondefinition.DisplayedSessionSelector;
import com.termux.app.sessiondefinition.SessionDefinitionCapCountPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionPlannedSession;
import com.termux.app.sessiondefinition.SessionInputDeliverabilityDwell;
import com.termux.app.sessiondefinition.SessionResourceReleasePlanner;
import com.termux.app.sessiondefinition.VisibleSessionSelector;
import com.termux.app.sessiondefinition.SessionDefinitionPlanner;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.session.AlwaysPresentSessionPlanner;
import com.termux.app.terminal.session.AlwaysPresentSessionStartup;
import com.termux.app.terminal.session.AlwaysPresentSessionStartupPlanner;
import com.termux.app.terminal.session.DuplicateSessionNameResolution;
import com.termux.app.terminal.session.FinishedSessionEnterAction;
import com.termux.app.terminal.session.TransientCommandSessionName;
import com.termux.app.terminal.session.DuplicateSessionNameResolver;
import com.termux.app.terminal.session.PersistedSession;
import com.termux.app.terminal.session.SessionReconnectPacer;
import com.termux.app.terminal.session.UserRemovedSessionReconnectSuppressionPlanner;
import com.termux.app.terminal.session.PersistedSessionRestoreData;
import com.termux.app.terminal.tts.TtsManager;
import com.termux.app.terminal.session.PersistedSessionSerializer;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.shared.termux.TermuxConstants;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.terminal.io.BellHandler;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.terminal.TextStyle;
import com.termux.view.TerminalView;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

/** The {@link TerminalSessionClient} implementation that may require an {@link Activity} for its interface methods. */
public class TermuxTerminalSessionActivityClient extends TermuxTerminalSessionClientBase {

    private final TermuxActivity mActivity;

    private final PersistedSessionSerializer mPersistedSessionSerializer = new PersistedSessionSerializer();

    private final SessionNameBrowserTabUrlResolver mSessionNameBrowserTabUrlResolver = new SessionNameBrowserTabUrlResolver();

    private final SessionOutputProgressTracker mSessionOutputProgressTracker = new SessionOutputProgressTracker();

    private final BackgroundOutputScanGate mBackgroundOutputScanGate = new BackgroundOutputScanGate();

    private final AllSessionsStatuslineScanGate mAllSessionsStatuslineScanGate =
        new AllSessionsStatuslineScanGate();

    private final AllSessionsStatuslineParser mAllSessionsStatuslineParser =
        new AllSessionsStatuslineParser();

    @Nullable
    private HandlerThread mStatuslineParseThread;

    @Nullable
    private Handler mStatuslineParseHandler;

    private final AlwaysPresentSessionPlanner mAlwaysPresentSessionPlanner = new AlwaysPresentSessionPlanner();
    private final AlwaysPresentSessionStartupPlanner mAlwaysPresentSessionStartupPlanner = new AlwaysPresentSessionStartupPlanner();

    private final LinkedHashMap<TerminalSession, PersistedSession> mPersistedSessionBySession = new LinkedHashMap<>();

    private boolean mStartupDisplayedSessionSelectionPending;

    private final SessionDefinitionCapCountPlanner mCapCountPlanner = new SessionDefinitionCapCountPlanner();

    private int maxSessions() {
        return mActivity.getPreferences().getSessionDefinitionMaxSessions();
    }

    private int cappedSessionCount(@NonNull TermuxService service) {
        List<SessionDefinitionCapCountPlanner.CountedSession> countedSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            countedSessions.add(new SessionDefinitionCapCountPlanner.CountedSession(
                terminalSession == null ? null : terminalSession.mSessionName,
                terminalSession != null && terminalSession.isRunning()));
        }
        return mCapCountPlanner.countSessionsTowardCap(countedSessions, hiddenSessionNames());
    }

    @NonNull
    private Set<String> hiddenSessionNames() {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        return preferences == null ? Collections.emptySet() : preferences.getDisabledSessionNames();
    }

    private boolean isRecordedHidden(@Nullable String sessionName) {
        return HiddenSessionNameMatcher.matchesAHiddenSession(sessionName, hiddenSessionNames());
    }

    private void notifySessionLimitExceeded(int configuredLimit, int droppedSessionCount) {
        if (droppedSessionCount <= 0) return;
        DiagnosticEventLogHolder.record(DiagnosticEventType.MAX_SESSIONS_REACHED,
            "cap=" + configuredLimit + " dropped=" + droppedSessionCount);
        mActivity.showToast(mActivity.getString(R.string.msg_session_limit_exceeded, configuredLimit, droppedSessionCount), true);
    }

    private SoundPool mBellSoundPool;

    private int mBellSoundId;

    private static final String LOG_TAG = "TermuxTerminalSessionActivityClient";

    private static final float SESSION_NAME_BAR_TITLE_RELATIVE_SIZE = 0.79f;

    private static final int SESSION_NAME_BAR_TITLE_TEXT_COLOR = 0xFFFFFFFF;

    private static final long ACTIVE_SESSION_SEEN_TICK_INTERVAL_MILLIS = 1000L;

    private static final long ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS = 1500L;

    /**
     * Spacing between successive proactive background reconnects. Each background tick reconnects every
     * currently-stale non-current session, but spaced roughly one second apart so a large backlog never
     * opens all its file descriptors and ssh/tmux reattaches at the same instant.
     */
    static final long STAGGERED_RECONNECT_INTERVAL_MILLIS = 1000L;

    /**
     * How many displayed sessions' statuslines are read and reparsed in one main-thread batch of the
     * on-launch / on-reload displayed-session refresh ({@link
     * #repopulateStatuslineTimesForDisplayedSessions(boolean)}) and of the periodic displayed-session
     * call scan ({@link #refreshDisplayedSessionsForCallToUser()}). The displayed set can be as large
     * as the session cap, so the transcript reads are chunked into batches of this size, each
     * subsequent batch posted as its own main-thread message, so a launch, reload or periodic scan
     * with many displayed sessions never reads every transcript in one uninterrupted pass and never
     * blocks the UI thread for the whole set.
     */
    static final int STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE = 4;

    /**
     * The staleness threshold used to decide which non-current sessions the background tick reconnects.
     * A session whose last {@code out:} statusline time is older than this is treated as stale and is
     * reconnected on the next tick so its shown info is refreshed; a session receiving fresh output
     * stays under this age and is left untouched. It is deliberately shorter than {@link
     * HungSessionDetector#STALE_OUT_MAX_AGE_MILLIS} and tied to the background reconnect tick interval
     * (default five minutes, {@link
     * com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_APP#DEFAULT_VALUE_KEY_BACKGROUND_RECONNECT_SCAN_INTERVAL_MINUTES})
     * so that a stale session is refreshed within roughly one tick — a few minutes — instead of only
     * after the former ten-minute hung threshold plus a full tick, which let info reach 30+ minutes old.
     */
    static final long BACKGROUND_RECONNECT_STALE_OUT_MAX_AGE_MILLIS = 4L * 60L * 1000L;

    private static final long[] POST_RECONNECT_STATUSLINE_RESCAN_BACKOFF_MILLIS = {
        ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS, 3000L, 5000L, 8000L, 12000L};

    private final PostReconnectStatuslineRescanRetryPlanner mPostReconnectStatuslineRescanRetryPlanner =
        new PostReconnectStatuslineRescanRetryPlanner(POST_RECONNECT_STATUSLINE_RESCAN_BACKOFF_MILLIS);

    /**
     * How long a row is allowed to stay on the reconnecting spinner without any statusline data
     * arriving before the timeout fires. When it fires and the row is still reconnecting, a bounded
     * backoff retry ({@link #RECONNECT_RETRY_BACKOFF_MILLIS}) is attempted, and once those are
     * exhausted the row leaves the perpetual spinner for the "reconnect failed / tap to retry" state.
     */
    static final long RECONNECT_TIMEOUT_MILLIS = 30L * 1000L;

    /**
     * The bounded auto-retry backoff schedule applied after {@link #RECONNECT_TIMEOUT_MILLIS} elapses
     * while a row is still reconnecting. Each entry is the delay before the next re-attempt; the array
     * length caps the number of retries so a truly-dead remote settles on the failed state instead of
     * looping forever.
     */
    static final long[] RECONNECT_RETRY_BACKOFF_MILLIS = {5L * 1000L, 15L * 1000L};

    private final SessionReconnectRetryPlan mReconnectRetryPlan =
        new SessionReconnectRetryPlan(RECONNECT_TIMEOUT_MILLIS, RECONNECT_RETRY_BACKOFF_MILLIS);

    private final Map<String, Runnable> mReconnectTimeoutRunnableByName = new HashMap<>();

    /**
     * The sessions whose reconnect the owner asked for by tapping the row, either to retry a failed
     * one or to switch to it. Materializing the replacement emulator off-screen spawns that session's
     * subprocess on the main thread, which is affordable for the one session the owner is waiting on
     * and is what freezes the app when a bulk sweep does it for every session it reconnects. The name
     * stays here across the bounded auto-retry ladder so a retry of an owner-asked reconnect keeps
     * materializing, and is dropped when the reconnect settles.
     */
    private final Set<String> mSessionNamesWhoseReconnectTheOwnerAsked = new HashSet<>();

    /**
     * The single session a bulk sweep materializes: the one whose unanswered owner call is oldest, so
     * the session the owner has been kept waiting on longest is the one that is ready to draw. Null
     * while no session in the sweep carries an unanswered call.
     */
    @Nullable
    private String mSessionNameToMaterializeOnTheNextBulkReconnect;

    private static final long MILLIS_PER_MINUTE = 60L * 1000L;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final DeadSessionReconnectPlanner mDeadSessionReconnectPlanner = new DeadSessionReconnectPlanner();
    private final UserRemovedSessionReconnectSuppressionPlanner mUserRemovedSessionReconnectSuppressionPlanner =
        new UserRemovedSessionReconnectSuppressionPlanner();

    private final VisibleSessionSelector mVisibleSessionSelector = new VisibleSessionSelector();

    private final DisplayedSessionSelector mDisplayedSessionSelector = new DisplayedSessionSelector();

    private final SessionResourceReleasePlanner mSessionResourceReleasePlanner =
        new SessionResourceReleasePlanner();

    private final SessionReconnectPacer mSessionReconnectPacer = new SessionReconnectPacer(
        mMainThreadHandler::postDelayed,
        this::isSessionStillInTheReconnectList,
        this::reconnectThenMarkSessionReconnecting);

    private final HungSessionDetector mHungSessionDetector =
        new HungSessionDetector(BACKGROUND_RECONNECT_STALE_OUT_MAX_AGE_MILLIS);

    private final SessionInputDeliverabilityDwell mSessionInputDeliverabilityDwell =
        new SessionInputDeliverabilityDwell();

    private final Runnable mActiveSessionSeenTickRunnable = this::onActiveSessionSeenTick;

    private boolean mActiveSessionSeenTickScheduled;

    private final Runnable mAllSessionsCallScanTickRunnable = this::onAllSessionsCallScanTick;

    private boolean mAllSessionsCallScanTickScheduled;

    private final Runnable mDisplayedSessionCallScanTickRunnable = this::onDisplayedSessionCallScanTick;

    private boolean mDisplayedSessionCallScanTickScheduled;

    public TermuxTerminalSessionActivityClient(TermuxActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Should be called when mActivity.onCreate() is called
     */
    public void onCreate() {
        // Set terminal fonts and colors
        checkForFontAndColors();
    }

    /**
     * Should be called when mActivity.onStart() is called
     */
    public void onStart() {
        // The service has connected, but data may have changed since we were last in the foreground.
        // Get the session stored in shared preferences stored by {@link #onStop} if its valid,
        // otherwise get the last session currently running. A session already displayed and still
        // live is preserved, so returning to the foreground never yanks the user away from the
        // session they are working in.
        if (mActivity.getTermuxService() != null) {
            if (shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession()))
                setStartupDisplayedSession(getCurrentStoredSessionOrLast());
            termuxSessionListNotifyUpdated();
            mActivity.prewarmSessionDefinitionDocument();
            mActivity.eagerLoadAllSessions();
        }

        // The current terminal session may have changed while being away, force
        // a refresh of the displayed terminal.
        mActivity.getTerminalView().onScreenUpdated();
        openTagsForSession(mActivity.getCurrentSession());
        backgroundOutputTagsForSession(mActivity.getCurrentSession());

        // The pass runs once now for sessions already loaded, once more after the staged
        // eager-load creates the remaining emulators, and once again after a short delay so that
        // statuslines that render late (alternate-screen restoration, scrolled output, or a screen
        // not yet drawn at onStart) are captured. The store's unchanged-statusline short-circuit
        // makes the repeated passes idempotent for sessions already recorded.
        repopulateStatuslineTimesForAllSessions();
        mMainThreadHandler.post(this::repopulateStatuslineTimesForAllSessions);
        mMainThreadHandler.postDelayed(this::repopulateStatuslineTimesForAllSessions,
            ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS);

        // The narrow passes above only populate the foreground/on-screen sessions; force a
        // displayed-session refresh so every displayed row shows current content right after launch
        // without a manual Send. Re-posted after the on-load delay to capture late-rendering statuslines.
        repopulateStatuslineTimesForDisplayedSessions(true);
        mMainThreadHandler.postDelayed(
            () -> repopulateStatuslineTimesForDisplayedSessions(true), ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS);
    }

    /**
     * Should be called when mActivity.onResume() is called
     */
    public void onResume() {
        // Just initialize the mBellSoundPool and load the sound, otherwise bell might not run
        // the first time bell key is pressed and play() is called, since sound may not be loaded
        // quickly enough before the call to play(). https://stackoverflow.com/questions/35435625
        loadBellSoundPool();

        startActiveSessionSeenTick();
        startAllSessionsCallScanTick();
        startDisplayedSessionCallScanTick();
    }

    /**
     * Should be called when mActivity.onStop() is called
     */
    public void onStop() {
        stopActiveSessionSeenTick();
        stopAllSessionsCallScanTick();
        stopDisplayedSessionCallScanTick();
        stopStatuslineParseThread();

        // Store current session in shared preferences so that it can be restored later in
        // {@link #onStart} if needed.
        setCurrentStoredSession();

        // Release mBellSoundPool resources, specially to prevent exceptions like the following to be thrown
        // java.util.concurrent.TimeoutException: android.media.SoundPool.finalize() timed out after 10 seconds
        // Bell is not played in background anyways
        // Related: https://stackoverflow.com/a/28708351/14686958
        releaseBellSoundPool();
    }

    /**
     * Should be called when mActivity.reloadActivityStyling() is called
     */
    public void onReloadActivityStyling() {
        // Set terminal fonts and colors
        checkForFontAndColors();
    }



    @Override
    public void onGenuineOutput(@NonNull TerminalSession changedSession) {
        // onGenuineOutput fires only on genuine process output past the stripped keystroke echo,
        // regardless of whether the main or the alternate screen buffer is active, and never from
        // scrolling, viewport changes, or redraws. Recording output activity directly on every such
        // event keeps a full-screen alternate-buffer program's out: time advancing while it emits
        // output, without keying on committed scrollback growth (which never advances in the
        // alternate buffer). Scrolling the terminal view does not reach this path and so does not
        // advance the out: time.
        recordOutputActivityForSession(changedSession);
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        // The explicit-call and app-update tags MUST be detected for every session that produces
        // output, not only the session currently being viewed, so that a non-current or backgrounded
        // session that calls the user records its red dot without the owner having to open it. These
        // run regardless of which session is current and regardless of activity visibility.
        SessionOutputScanText scanText = shouldRunBackgroundOutputScan(changedSession)
            ? readOutputScanTextOnce(changedSession)
            : null;
        if (scanText != null) {
            parseStatuslineAndScanOutputTagsOffThread(scanText, System.currentTimeMillis());
        }

        if (!mActivity.isVisible()) return;

        if (mActivity.getCurrentSession() == changedSession) {
            mActivity.getTerminalView().onScreenUpdated();
            updateSessionNameOverlay();
            // The open-URL tag stays scoped to the current session only: auto-opening a URL is a
            // foreground action and opening a non-viewed session's URL would be jarring.
            openTagsForSession(changedSession,
                scanText == null ? null : scanText.getTranscriptText());
        }
    }

    private void openTagsForSession(TerminalSession session,
                                    @Nullable String transcriptTextAlreadyMaterialized) {
        if (session == null) return;

        OpenTagBrowserController openTagBrowserController = mActivity.getOpenTagBrowserController();
        if (openTagBrowserController == null) return;
        if (!openTagBrowserController.isAutoOpenEnabled()) return;

        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;

        TerminalBuffer screen = emulator.getScreen();
        if (screen == null) return;

        long scanStartNanos = System.nanoTime();
        int transcriptRows = screen.getActiveTranscriptRows();

        String transcriptText = transcriptTextAlreadyMaterialized != null
            ? transcriptTextAlreadyMaterialized
            : screen.getTranscriptText();
        openTagBrowserController.onSessionTextChanged(session.mHandle, transcriptText);

        AppOpenTagController appOpenTagController = mActivity.getAppOpenTagController();
        if (appOpenTagController != null && appOpenTagController.isAutoOpenEnabled())
            appOpenTagController.onSessionTextChanged(session.mHandle, transcriptText);

        ForegroundOpenTagScanCostCounterHolder.getInstance()
            .record(System.nanoTime() - scanStartNanos, transcriptRows);
    }

    private boolean shouldRunBackgroundOutputScan(@Nullable TerminalSession session) {
        if (session == null) return false;
        if (session.mHandle == null) return true;
        return mBackgroundOutputScanGate.shouldScan(
            session.mHandle, session.getScreenContentVersion(), System.currentTimeMillis());
    }

    @VisibleForTesting
    public void forgetBackgroundOutputScanThrottle(@Nullable TerminalSession session) {
        if (session == null || session.mHandle == null) return;
        mBackgroundOutputScanGate.forget(session.mHandle);
    }

    private void backgroundOutputTagsForSession(TerminalSession session) {
        if (session == null) return;
        SessionOutputScanText scanText = readOutputScanTextOnce(session);
        if (scanText == null) return;
        parseStatuslineAndScanOutputTagsOffThread(scanText, System.currentTimeMillis());
    }

    private void openTagsForSession(TerminalSession session) {
        openTagsForSession(session, null);
    }

    @Nullable
    private SessionOutputScanText readOutputScanTextOnce(@NonNull TerminalSession session) {
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return null;
        TerminalBuffer screen = emulator.getScreen();
        if (screen == null) return null;
        long scanStartNanos = System.nanoTime();
        int transcriptRows = screen.getActiveTranscriptRows();
        String transcriptText = screen.getTranscriptText();
        SessionOutputScanText scanText = new SessionOutputScanText(session.mSessionName,
            session.mHandle, transcriptText,
            SessionStatuslineScanText.reusingActiveBufferTranscript(emulator, screen, transcriptText));
        BackgroundOutputScanCostCounterHolder.getInstance()
            .record(System.nanoTime() - scanStartNanos, transcriptRows);
        return scanText;
    }

    private void parseStatuslineAndScanOutputTagsOffThread(@NonNull SessionOutputScanText scanText,
                                                           long nowMillis) {
        String sessionName = scanText.getSessionName();
        List<AllSessionsStatuslineParser.SessionScreenText> sessionScreenTexts = sessionName == null
            ? Collections.emptyList()
            : Collections.singletonList(new AllSessionsStatuslineParser.SessionScreenText(
                sessionName, scanText.getStatuslineScanText()));
        String sessionHandle = scanText.getSessionHandle();
        String transcriptText = scanText.getTranscriptText();
        BackgroundOutputTagScanner scanner = new BackgroundOutputTagScanner(
            mActivity.getCallToUserTagController(),
            mActivity.getUpdateTagUpdateController());
        TimeZone timeZone = TimeZone.getDefault();
        statuslineParseHandler().post(() -> {
            List<ParsedStatuslineUpdate> updates =
                mAllSessionsStatuslineParser.parse(sessionScreenTexts, nowMillis, timeZone);
            mMainThreadHandler.post(() -> {
                if (!updates.isEmpty()) {
                    applyStatuslineUpdates(updates);
                }
                boolean shouldScanCallToUserTag = shouldScanCallToUserTagForSessionName(sessionName);
                if (shouldScanCallToUserTag) {
                    recordCallToUserTagScanPerformedForSessionName(sessionName);
                }
                statuslineParseHandler().post(
                    () -> scanner.scan(sessionHandle, transcriptText, shouldScanCallToUserTag));
            });
        });
    }

    private static final class SessionOutputScanText {

        @Nullable
        private final String mSessionName;

        @Nullable
        private final String mSessionHandle;

        @NonNull
        private final String mTranscriptText;

        @NonNull
        private final String mStatuslineScanText;

        private SessionOutputScanText(@Nullable String sessionName,
                                      @Nullable String sessionHandle,
                                      @NonNull String transcriptText,
                                      @NonNull String statuslineScanText) {
            mSessionName = sessionName;
            mSessionHandle = sessionHandle;
            mTranscriptText = transcriptText;
            mStatuslineScanText = statuslineScanText;
        }

        @Nullable
        String getSessionName() {
            return mSessionName;
        }

        @Nullable
        String getSessionHandle() {
            return mSessionHandle;
        }

        @NonNull
        String getTranscriptText() {
            return mTranscriptText;
        }

        @NonNull
        String getStatuslineScanText() {
            return mStatuslineScanText;
        }
    }

    private void recordCallToUserTagScanPerformedForSessionName(@Nullable String sessionName) {
        if (sessionName == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.recordCallToUserTagScanPerformed(sessionName);
    }

    private boolean shouldScanCallToUserTagForSessionName(@Nullable String sessionName) {
        if (sessionName == null) return true;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return true;
        return store.shouldScanCallToUserTag(sessionName);
    }

    /**
     * Repopulates each already-loaded session's statusline-derived call/out/reply times and
     * call-to-user pending state from the session's current on-screen statusline, on the app-load /
     * reconnect path. The persisted activity store lives under the OS cache directory and is cleared
     * on app update, so after a restart the store loads empty and an idle session (one that has not
     * produced output since the restart) keeps null times and no unread mark until it is re-scanned.
     * This one-time pass over current statuslines reads the same {@code call:}/{@code out:}/{@code
     * reply:} tokens a live scan reads and feeds them through the same {@link
     * SessionNewActivityStore#recordStatuslineTimes} path, so the times and the unread (call-to-user)
     * mark are correct immediately after a cold start without waiting for each session to emit output.
     * It is not on the per-keystroke path, so it does not affect input latency.
     */
    private void repopulateStatuslineTimesForAllSessions() {
        repopulateStatuslineTimesForAllSessions(false);
    }

    /**
     * The {@code forceRescan} variant bypasses the {@link AllSessionsStatuslineScanGate}
     * content-version skip so every session's statusline is read and reparsed even when its screen has
     * not changed since the previous pass. The ordinary tick keeps the gate so an idle session whose
     * screen is unchanged is skipped, but the on-demand reload press and the periodic reconnect tick
     * must repopulate every session's call/out/reply tier regardless of the gate, because a session
     * whose store entry was never populated keeps an unchanged screen content version and would
     * otherwise be skipped forever and stay on the uncolored tier. The gate's recorded version is still
     * updated on a forced pass so a later gated tick is not redundantly forced to rescan the same
     * unchanged screen. The transcript materialization and the regex parse stay off the main thread via
     * {@link #parseAndApplyStatuslineUpdatesOffThread}.
     */
    private void repopulateStatuslineTimesForAllSessions(boolean forceRescan) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;

        long nowMillis = System.currentTimeMillis();
        Set<String> visibleSessionNames = visibleSessionNames();
        if (forceRescan) {
            visibleSessionNames =
                firstForcedRescanBatchAfterDeferringTheRest(new ArrayList<>(visibleSessionNames));
        }
        List<AllSessionsStatuslineParser.SessionScreenText> sessionScreenTexts = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession session = termuxSession.getTerminalSession();
            if (session == null || session.mSessionName == null) continue;
            if (!visibleSessionNames.contains(session.mSessionName)) continue;
            TerminalEmulator emulator = session.getEmulator();
            if (emulator == null) continue;
            TerminalBuffer screen = emulator.getScreen();
            if (screen == null) continue;
            long screenContentVersion = emulator.getScreenContentVersion();
            if (forceRescan) {
                mAllSessionsStatuslineScanGate.markScanned(session.mHandle, screenContentVersion);
            } else if (!mAllSessionsStatuslineScanGate.shouldScan(session.mHandle, screenContentVersion,
                store.hasStoredStatuslineData(session.mSessionName))) {
                continue;
            }
            sessionScreenTexts.add(new AllSessionsStatuslineParser.SessionScreenText(
                session.mSessionName, statuslineScanText(emulator, screen)));
        }
        if (sessionScreenTexts.isEmpty()) {
            return;
        }
        parseAndApplyStatuslineUpdatesOffThread(sessionScreenTexts, nowMillis);
    }

    /**
     * Refreshes the displayed content (call/out/reply statusline tier) of every DISPLAYED session under
     * the normal applied filter — visible (not hidden) AND under an expanded (non-collapsed) project,
     * plus the current session unconditionally — the same set the fast displayed-session call-scan cycle
     * uses — rather than only the narrow foreground/on-screen {@link #visibleSessionNames()} set the
     * ordinary rescan
     * covers. This is the on-launch and on-reload counterpart to the manual per-session refresh the
     * owner otherwise triggers by switching to each session: it reproduces only the statusline
     * display-refresh effect (reading and reparsing each session's current screen through the same
     * {@link SessionNewActivityStore#recordStatuslineTimes} path), and never writes any input to any
     * session. The displayed set can be as large as the session cap, so the per-session transcript
     * reads are split into small batches, each subsequent batch posted to the main-thread {@link
     * Handler} one {@link SessionReconnectPacer#MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS} further out
     * than the batch before it, so a launch or reload with many displayed sessions never reads all
     * their transcripts in one main-thread pass and each batch actually yields a frame. A zero delay
     * would not: {@code postDelayed(runnable, 0)} is {@code post(runnable)}, so every batch would be
     * due at the same instant and the looper would drain them consecutively, which is the whole-set
     * main-thread pass this batching exists to prevent. The spacing the owner ruled out was one
     * second per batch, which made the last displayed session wait seconds for its own refresh; one
     * frame per batch costs the last of sixteen displayed sessions about 48 milliseconds. Each
     * batch forces the gate-bypassing rescan through {@link
     * #repopulateStatuslineTimesForSessionNames(Set, boolean)}, whose heavy transcript materialization
     * and regex parse still run off the main thread via {@link #parseAndApplyStatuslineUpdatesOffThread}.
     */
    private void repopulateStatuslineTimesForDisplayedSessions(boolean forceRescan) {
        List<String> displayedSessionNames = new ArrayList<>(displayedSessionNames());
        if (displayedSessionNames.isEmpty()) return;
        List<Set<String>> batches = StaggeredStatuslineRescanBatchPlanner.sessionNameBatches(
            displayedSessionNames, STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE);
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            Set<String> batchSessionNames = batches.get(batchIndex);
            if (batchIndex == 0) {
                repopulateStatuslineTimesForSessionNames(batchSessionNames, forceRescan);
            } else {
                mMainThreadHandler.postDelayed(
                    () -> repopulateStatuslineTimesForSessionNames(batchSessionNames, forceRescan),
                    batchIndex * SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS);
            }
        }
    }

    @NonNull
    private Set<String> firstForcedRescanBatchAfterDeferringTheRest(
            @NonNull List<String> sessionNames) {
        List<StaggeredStatuslineRescanBatchPlanner.Batch> batches =
            StaggeredStatuslineRescanBatchPlanner.plan(sessionNames,
                STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE, STAGGERED_RECONNECT_INTERVAL_MILLIS);
        if (batches.isEmpty()) return Collections.emptySet();
        for (int batchIndex = 1; batchIndex < batches.size(); batchIndex++) {
            StaggeredStatuslineRescanBatchPlanner.Batch batch = batches.get(batchIndex);
            mMainThreadHandler.postDelayed(
                () -> repopulateStatuslineTimesForSessionNames(batch.getSessionNames(), true),
                batch.getDelayMillis());
        }
        return batches.get(0).getSessionNames();
    }

    /**
     * The shared body of {@link #repopulateStatuslineTimesForAllSessions(boolean)} and the per-batch
     * {@link #repopulateStatuslineTimesForDisplayedSessions(boolean)} pass. When {@code forceRescan} is
     * set it bypasses the {@link AllSessionsStatuslineScanGate} content-version skip so every named
     * session's statusline is read and reparsed even when its screen is unchanged (still recording the
     * scanned version on the gate); otherwise it keeps the gate so an idle unchanged session is skipped.
     * A session whose store entry was never populated keeps an unchanged screen content version and
     * would otherwise be skipped forever and stay on the uncolored tier, which is why the on-launch and
     * on-reload paths force the rescan. Only the {@code call:}/{@code out:}/{@code reply:} tokens are
     * read and reparsed; no input is written to any session. The transcript materialization and the
     * regex parse stay off the main thread via {@link #parseAndApplyStatuslineUpdatesOffThread}.
     */
    private void repopulateStatuslineTimesForSessionNames(@NonNull Set<String> sessionNames,
                                                          boolean forceRescan) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;

        long nowMillis = System.currentTimeMillis();
        List<AllSessionsStatuslineParser.SessionScreenText> sessionScreenTexts = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession session = termuxSession.getTerminalSession();
            if (session == null || session.mSessionName == null) continue;
            if (!sessionNames.contains(session.mSessionName)) continue;
            TerminalEmulator emulator = session.getEmulator();
            if (emulator == null) continue;
            TerminalBuffer screen = emulator.getScreen();
            if (screen == null) continue;
            long screenContentVersion = emulator.getScreenContentVersion();
            if (forceRescan) {
                mAllSessionsStatuslineScanGate.markScanned(session.mHandle, screenContentVersion);
            } else if (!mAllSessionsStatuslineScanGate.shouldScan(session.mHandle, screenContentVersion,
                store.hasStoredStatuslineData(session.mSessionName))) {
                continue;
            }
            sessionScreenTexts.add(new AllSessionsStatuslineParser.SessionScreenText(
                session.mSessionName, statuslineScanText(emulator, screen)));
        }
        if (sessionScreenTexts.isEmpty()) {
            return;
        }
        parseAndApplyStatuslineUpdatesOffThread(sessionScreenTexts, nowMillis);
    }

    /**
     * Runs the expensive per-session statusline regex parse on the background parse thread and
     * marshals the resulting {@link SessionNewActivityStore} mutations and the session-list UI refresh
     * back to the main thread. The terminal emulator is not thread-safe, so every emulator read (the
     * gate's content-version check and the transcript extraction in {@link #statuslineScanText}) has
     * already happened on the main thread before this is called; only the pure-CPU token parse over the
     * already-materialized screen-text strings runs off the main thread, and the store mutation is
     * applied on the main Looper so the store stays single-threaded.
     */
    private void parseAndApplyStatuslineUpdatesOffThread(
            @NonNull List<AllSessionsStatuslineParser.SessionScreenText> sessionScreenTexts,
            long nowMillis) {
        TimeZone timeZone = TimeZone.getDefault();
        statuslineParseHandler().post(() -> {
            List<ParsedStatuslineUpdate> updates =
                mAllSessionsStatuslineParser.parse(sessionScreenTexts, nowMillis, timeZone);
            if (updates.isEmpty()) {
                return;
            }
            mMainThreadHandler.post(() -> applyStatuslineUpdates(updates));
        });
    }

    private void applyStatuslineUpdates(@NonNull List<ParsedStatuslineUpdate> updates) {
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        for (ParsedStatuslineUpdate update : updates) {
            update.applyTo(store);
            store.clearReconnecting(update.getSessionName());
            cancelReconnectTimeout(update.getSessionName());
        }
        termuxSessionListNotifyUpdated();
    }

    @NonNull
    private Handler statuslineParseHandler() {
        if (mStatuslineParseThread == null || mStatuslineParseHandler == null) {
            HandlerThread parseThread = new HandlerThread("TermuxStatuslineParse");
            parseThread.start();
            mStatuslineParseThread = parseThread;
            mStatuslineParseHandler = new Handler(parseThread.getLooper());
        }
        return mStatuslineParseHandler;
    }

    private void stopStatuslineParseThread() {
        if (mStatuslineParseHandler != null) {
            mStatuslineParseHandler.removeCallbacksAndMessages(null);
            mStatuslineParseHandler = null;
        }
        if (mStatuslineParseThread != null) {
            mStatuslineParseThread.quit();
            mStatuslineParseThread = null;
        }
    }

    @NonNull
    private static String statuslineScanText(@NonNull TerminalEmulator emulator,
                                             @NonNull TerminalBuffer screen) {
        return SessionStatuslineScanText.of(emulator, screen);
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession updatedSession) {
        if (!mActivity.isVisible()) return;

        if (updatedSession != mActivity.getCurrentSession()) {
            // Only show toast for other sessions than the current one, since the user
            // probably consciously caused the title change to change in the current session
            // and don't want an annoying toast for that.
            mActivity.showToast(toToastTitle(updatedSession), true);
        }

        termuxSessionListNotifyUpdated();
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
        TermuxService service = mActivity.getTermuxService();

        if (service == null || service.wantsToStop()) {
            // The service wants to stop as soon as possible.
            mActivity.finishActivityIfNotFinishing();
            return;
        }

        if (mActivity.getOpenTagBrowserController() != null)
            mActivity.getOpenTagBrowserController().forgetSession(finishedSession.mHandle);

        if (mActivity.getAppOpenTagController() != null)
            mActivity.getAppOpenTagController().forgetSession(finishedSession.mHandle);

        if (mActivity.getUpdateTagUpdateController() != null)
            mActivity.getUpdateTagUpdateController().forgetSession(finishedSession.mHandle);

        if (mActivity.getCallToUserTagController() != null)
            mActivity.getCallToUserTagController().forgetSession(finishedSession.mHandle);

        if (finishedSession.mHandle != null) {
            mBackgroundOutputScanGate.forget(finishedSession.mHandle);
            mAllSessionsStatuslineScanGate.forget(finishedSession.mHandle);
        }

        if (TransientCommandSessionName.isTransient(finishedSession.mSessionName)) {
            removeFinishedSession(finishedSession);
            return;
        }

        int index = service.getIndexOfSession(finishedSession);

        // For plugin commands that expect the result back, we should immediately close the session
        // and send the result back instead of waiting fo the user to press enter.
        // The plugin can handle/show errors itself.
        boolean isPluginExecutionCommandWithPendingResult = false;
        TermuxSession termuxSession = service.getTermuxSession(index);
        if (termuxSession != null) {
            isPluginExecutionCommandWithPendingResult = termuxSession.getExecutionCommand().isPluginExecutionCommandWithPendingResult();
            if (isPluginExecutionCommandWithPendingResult)
                Logger.logVerbose(LOG_TAG, "The \"" + finishedSession.mSessionName + "\" session will be force finished automatically since result in pending.");
        }

        if (mActivity.isVisible() && finishedSession != mActivity.getCurrentSession()) {
            // Show toast for non-current sessions that exit.
            // Verify that session was not removed before we got told about it finishing:
            if (index >= 0)
                mActivity.showToast(toToastTitle(finishedSession) + " - exited", true);
        }

        boolean isAndroidTV = mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
        if (shouldRemoveFinishedSession(isAndroidTV, service.getTermuxSessionsSize(), isPluginExecutionCommandWithPendingResult)) {
            removeFinishedSession(finishedSession);
        }
    }

    static boolean shouldRemoveFinishedSession(boolean isAndroidTV, int sessionsSize, boolean isPluginExecutionCommandWithPendingResult) {
        if (isPluginExecutionCommandWithPendingResult) {
            return true;
        }
        if (isAndroidTV) {
            // On Android TV devices we need to use older behaviour because we may
            // not be able to have multiple launcher icons.
            return sessionsSize > 1;
        }
        return false;
    }

    @Override
    public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
        if (!mActivity.isVisible()) return;

        ShareUtils.copyTextToClipboard(mActivity, text);
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
        if (!mActivity.isVisible()) return;

        String text = ShareUtils.getTextStringFromClipboardIfSet(mActivity, true);
        if (text != null)
            mActivity.getTerminalView().mEmulator.paste(text);
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
        // Record output activity only when genuinely new process output accompanies the bell, using
        // the same genuine-output gate as onTextChanged. A bell is delivered on every BEL byte,
        // including bells echoed back from the user's own keystrokes and repeated bells that carry no
        // new process output; recording output activity unconditionally on each bell pinned the
        // out: time to "now" on every refresh, so it never advanced past 0 seconds.
        recordNewOutputActivityForSession(session);

        if (!mActivity.isVisible()) return;

        switch (mActivity.getProperties().getBellBehaviour()) {
            case TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_VIBRATE:
                BellHandler.getInstance(mActivity).doBell();
                break;
            case TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_BEEP:
                loadBellSoundPool();
                if (mBellSoundPool != null)
                    mBellSoundPool.play(mBellSoundId, 1.f, 1.f, 1, 0, 1.f);
                break;
            case TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_IGNORE:
                // Ignore the bell character.
                break;
        }
    }

    @Override
    public void onSpeakNotification(@NonNull TerminalSession session, @NonNull String text) {
        if (mActivity.getCurrentSession() != session) return;
        if (!mActivity.getPreferences().isSpeakTagAutoReadEnabled()) return;
        TtsManager ttsManager = mActivity.getTtsManager();
        if (ttsManager == null) return;
        ttsManager.speak(text);
    }

    private void recordNewOutputActivityForSession(@NonNull TerminalSession session) {
        if (session.mSessionName == null) return;
        if (!mSessionOutputProgressTracker.hasNewOutput(
                session.mSessionName, session.getCommittedOutputLineCount())) {
            return;
        }
        recordOutputActivityForSession(session);
    }

    private void recordOutputActivityForSession(@NonNull TerminalSession session) {
        if (session.mSessionName == null) return;
        long nowMillis = System.currentTimeMillis();
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.recordOutputActivity(session.mSessionName, nowMillis);
        // The genuinely-viewed session (app visible AND this is the current session) is seen
        // unconditionally, so output — or a call — arriving while the user is present is immediately
        // marked seen and leaves no leftover indicator when the user switches away. Seen is never
        // recorded here for a non-viewed session, so a call to a non-viewed session keeps its red dot
        // until the user genuinely switches to it.
        if (isCurrentlyViewedSession(session))
            store.recordSeen(session.mSessionName, nowMillis);
        termuxSessionListNotifyUpdated();
    }

    private boolean isCurrentlyViewedSession(@NonNull TerminalSession session) {
        return mActivity.isVisible() && mActivity.getCurrentSession() == session;
    }

    /**
     * Whether a session is currently displayed in the terminal view and is still a live session of
     * the bound service. Reconnect, reload and restore paths use this to avoid yanking the user away
     * from the session they are actively working in: they only set the displayed session when none
     * is already displayed.
     */
    private boolean hasValidCurrentDisplayedSession() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;
        TerminalSession currentSession = mActivity.getCurrentSession();
        return currentSession != null && service.getIndexOfSession(currentSession) >= 0;
    }

    /**
     * Whether a reconnect, reload or restore path should switch the displayed session. The displayed
     * session MUST only change due to an explicit user selection, so these background paths switch
     * only when no valid session is already displayed (e.g. the very first session at startup).
     */
    static boolean shouldSwitchSessionOnReconnect(boolean hasValidCurrentDisplayedSession) {
        return !hasValidCurrentDisplayedSession;
    }

    @Nullable
    private String activeSessionName() {
        TerminalSession currentSession = mActivity.getCurrentSession();
        return currentSession == null ? null : currentSession.mSessionName;
    }

    @NonNull
    private Set<String> visibleSessionNames() {
        SessionListBottomSheetController sessionListBottomSheetController =
            mActivity.getSessionListBottomSheetController();
        boolean sessionListOpen =
            sessionListBottomSheetController != null && sessionListBottomSheetController.isOpen();
        List<String> onScreenListSessionNames = sessionListOpen
            ? sessionListBottomSheetController.getOnScreenSessionNames()
            : Collections.emptyList();
        return mVisibleSessionSelector.selectVisibleSessionNames(mActivity.isVisible(),
            activeSessionName(), sessionListOpen, onScreenListSessionNames, hiddenSessionNames());
    }

    @NonNull
    private Set<String> displayedSessionNames() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) {
            return Collections.emptySet();
        }
        List<String> allLiveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null || terminalSession.mSessionName == null) continue;
            allLiveSessionNames.add(terminalSession.mSessionName);
        }
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        Set<String> hiddenSessionNames = preferences != null
            ? preferences.getDisabledSessionNames()
            : Collections.emptySet();
        TermuxSessionsListViewController listViewController =
            mActivity.getTermuxSessionListViewController();
        Set<String> expandedProjectSessionNames = listViewController != null
            ? listViewController.getExpandedProjectSessionNames()
            : null;
        return mDisplayedSessionSelector.selectDisplayedSessionNames(mActivity.isVisible(),
            activeSessionName(), allLiveSessionNames, true, hiddenSessionNames,
            expandedProjectSessionNames);
    }

    private void purgeNewActivityForRemovedSession(@Nullable String sessionName) {
        if (sessionName == null) return;
        mSessionOutputProgressTracker.forget(sessionName);
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.purgeSession(sessionName);
        termuxSessionListNotifyUpdated();
    }

    private void purgeNewActivityForReconnectedSession(@Nullable String sessionName) {
        if (sessionName == null) return;
        mSessionOutputProgressTracker.forget(sessionName);
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.purgeSessionKeepingTheCallAndReplyTimes(sessionName);
        termuxSessionListNotifyUpdated();
    }

    /**
     * Marks {@code sessionName} as reconnecting so its bottom-sheet row shows the spinner for exactly
     * as long as the real reconnect/fetch is in flight: the flag is cleared when the next parsed
     * statusline for the session arrives ({@link #applyStatuslineUpdates}), with no timer. It is set
     * after {@link #reconnectDeadSessionPreservingDisplayedSession} returns, because that call clears
     * the flag via {@link SessionNewActivityStore#purgeSessionKeepingTheCallAndReplyTimes} while tearing
     * the dead session down; setting after the teardown leaves the fresh flag in place for the
     * just-recreated session.
     */
    private void markSessionReconnecting(@Nullable String sessionName,
                                         boolean theOwnerAskedForThisReconnect) {
        if (sessionName == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.setReconnecting(sessionName, System.currentTimeMillis());
        scheduleReconnectTimeout(sessionName);
        if (theOwnerAskedForThisReconnect) {
            mSessionNamesWhoseReconnectTheOwnerAsked.add(sessionName);
        }
        termuxSessionListNotifyUpdated();
    }

    private void scheduleReconnectTimeout(@NonNull String sessionName) {
        cancelReconnectTimeout(sessionName);
        Runnable timeoutRunnable = () -> onReconnectTimeout(sessionName);
        mReconnectTimeoutRunnableByName.put(sessionName, timeoutRunnable);
        mMainThreadHandler.postDelayed(timeoutRunnable, mReconnectRetryPlan.getTimeoutMillis());
    }

    /**
     * Cancels any pending reconnect timeout or backoff-retry runnable for {@code sessionName}. Called
     * on every success path that clears the reconnecting flag (statusline arrival) and when the row is
     * hidden, so a settled row never leaks a queued timeout or races into a false failed state.
     */
    public void cancelReconnectTimeout(@Nullable String sessionName) {
        if (sessionName == null) return;
        mSessionNamesWhoseReconnectTheOwnerAsked.remove(sessionName);
        Runnable pending = mReconnectTimeoutRunnableByName.remove(sessionName);
        if (pending != null) {
            mMainThreadHandler.removeCallbacks(pending);
        }
    }

    private void onReconnectTimeout(@NonNull String sessionName) {
        mReconnectTimeoutRunnableByName.remove(sessionName);
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        SessionReconnectTimeoutOutcome outcome = mReconnectRetryPlan.onTimeout(store, sessionName);
        switch (outcome.getDecision()) {
            case NOT_RECONNECTING:
                return;
            case RETRY:
                Runnable retryRunnable = () -> retryReconnect(sessionName);
                mReconnectTimeoutRunnableByName.put(sessionName, retryRunnable);
                mMainThreadHandler.postDelayed(retryRunnable, outcome.getRetryBackoffMillis());
                return;
            case FAILED:
            default:
                mSessionNamesWhoseReconnectTheOwnerAsked.remove(sessionName);
                termuxSessionListNotifyUpdated();
        }
    }

    private void retryReconnect(@NonNull String sessionName) {
        mReconnectTimeoutRunnableByName.remove(sessionName);
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        if (!store.isReconnecting(sessionName)) return;
        TerminalSession session = findTerminalSessionByName(sessionName);
        if (session == null) return;
        boolean theOwnerAskedForThisReconnect =
            mSessionNamesWhoseReconnectTheOwnerAsked.contains(sessionName);
        reconnectDeadSessionPreservingDisplayedSession(session, theOwnerAskedForThisReconnect);
        markSessionReconnecting(sessionName, theOwnerAskedForThisReconnect);
    }

    /**
     * Re-initiates the reconnect for a row the owner tapped while it was showing the "reconnect
     * failed / tap to retry" state: the failed flag and the exhausted retry counter are cleared so the
     * row re-enters the reconnecting spinner with a fresh bounded-retry budget.
     */
    public void retryReconnectAfterFailure(@Nullable String sessionName) {
        if (sessionName == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.clearReconnectFailed(sessionName);
        store.resetReconnectRetryAttempt(sessionName);
        TerminalSession session = findTerminalSessionByName(sessionName);
        if (session == null) return;
        reconnectDeadSessionPreservingDisplayedSession(session, true);
        markSessionReconnecting(sessionName, true);
    }

    @Nullable
    private TerminalSession findTerminalSessionByName(@NonNull String sessionName) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) continue;
            if (sessionName.equals(terminalSession.mSessionName)) {
                return terminalSession;
            }
        }
        return null;
    }

    private void onActiveSessionSeenTick() {
        mActiveSessionSeenTickScheduled = false;
        if (!mActivity.isVisible()) return;

        recordActiveSessionSeen();
        termuxSessionListNotifyUpdated();
        scheduleActiveSessionSeenTick();
    }

    private void recordActiveSessionSeen() {
        // The genuinely-viewed current session's seen MUST advance every second unconditionally so
        // that activity occurring while the user is present is treated as seen and leaves no stale
        // indicator after the user switches away. This tick only ever records the current session
        // (activeSessionName resolves to getCurrentSession), so a non-viewed session's seen is never
        // advanced and its explicit-call red dot persists until the user genuinely switches to it.
        String sessionName = activeSessionName();
        if (sessionName == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.recordSeen(sessionName, System.currentTimeMillis());
    }

    public void startActiveSessionSeenTick() {
        if (mActivity.isVisible())
            recordActiveSessionSeen();
        scheduleActiveSessionSeenTick();
    }

    private void scheduleActiveSessionSeenTick() {
        if (mActiveSessionSeenTickScheduled) return;
        if (!mActivity.isVisible()) return;
        mActiveSessionSeenTickScheduled = true;
        mMainThreadHandler.postDelayed(mActiveSessionSeenTickRunnable,
            ACTIVE_SESSION_SEEN_TICK_INTERVAL_MILLIS);
    }

    public void stopActiveSessionSeenTick() {
        mActiveSessionSeenTickScheduled = false;
        mMainThreadHandler.removeCallbacks(mActiveSessionSeenTickRunnable);
    }

    /**
     * Periodically re-scans every session's current output buffer so that a background session
     * which already emitted its call-to-user marker and then went idle turns its red un-replied-call
     * indicator on without the owner having to open it. The per-session scan is otherwise driven only
     * by {@link #onTextChanged}, which fires only for a session producing fresh output, so an idle
     * background session whose last output landed before the indicator was armed is never re-scanned
     * until the activity is foregrounded ({@link #onStart}) or the session emits more output. This
     * repeating pass closes that gap by feeding every session's current statusline through the same
     * {@link #repopulateStatuslineTimesForAllSessions} path the on-load pass uses. The store's
     * unchanged-statusline short-circuit keeps each pass cheap, and the interval is read from the
     * {@code background-call-scan-interval-minutes} property ({@link
     * TermuxPropertyConstants#KEY_BACKGROUND_CALL_SCAN_INTERVAL_MINUTES}, defaulting to five minutes)
     * so the detection window can be tuned without rebuilding while keeping the main-thread cost
     * negligible. A changed value takes effect the next time the tick is scheduled. The tick runs only
     * while the activity is visible and is removed in {@link #onStop}, so it does not leak the runnable
     * or run in the background.
     */
    public void startAllSessionsCallScanTick() {
        if (mActivity.isVisible())
            repopulateStatuslineTimesForAllSessions();
        scheduleAllSessionsCallScanTick();
    }

    private void scheduleAllSessionsCallScanTick() {
        if (mAllSessionsCallScanTickScheduled) return;
        if (!mActivity.isVisible()) return;
        mAllSessionsCallScanTickScheduled = true;
        mMainThreadHandler.postDelayed(mAllSessionsCallScanTickRunnable,
            allSessionsCallScanIntervalMillis());
    }

    private long allSessionsCallScanIntervalMillis() {
        TermuxAppSharedProperties properties = mActivity.getProperties();
        int intervalMinutes = properties != null
            ? properties.getBackgroundCallScanIntervalMinutes()
            : TermuxPropertyConstants.DEFAULT_IVALUE_BACKGROUND_CALL_SCAN_INTERVAL_MINUTES;
        return intervalMinutes * MILLIS_PER_MINUTE;
    }

    public void stopAllSessionsCallScanTick() {
        mAllSessionsCallScanTickScheduled = false;
        mMainThreadHandler.removeCallbacks(mAllSessionsCallScanTickRunnable);
    }

    private void onAllSessionsCallScanTick() {
        mAllSessionsCallScanTickScheduled = false;
        if (!mActivity.isVisible()) return;
        repopulateStatuslineTimesForAllSessions();
        scheduleAllSessionsCallScanTick();
    }

    /**
     * Reconnects and full-call-scans every displayed (non-hidden) session the owner is monitoring on a
     * short (default one-minute) cycle so a call-to-user tag emitted by a displayed-but-not-foreground
     * session surfaces its red marker near-live instead of only at the next slow reconnect/call-scan
     * tick (which left displayed-but-not-foreground sessions undetected for up to ~23 minutes).
     *
     * <p>The slower {@link #startAllSessionsCallScanTick() all-sessions call-scan tick} only re-parses
     * the statusline of the narrow visible set and never pulls a dead session's freshly-accumulated
     * tmux output, so a displayed session whose call-to-user tag landed after its last reconnect is
     * neither refreshed nor transcript-scanned until it becomes foreground or the sheet opens. This
     * tick closes that gap: each cycle it reconnects the dead/hung displayed sessions (staggered via the
     * shared {@link #reconnectDeadDisplayedSessionsInBackground} path so ~24 sessions never reconnect at
     * once) so their accumulated output — including the call-to-user tag — is pulled, then runs the full
     * {@link BackgroundOutputTagScanner} call-to-user transcript scan over every displayed session. The
     * per-session {@link CallToUserTagScanner} dedup ensures an already-detected tag does not re-fire.
     *
     * <p>Hidden/filtered sessions are excluded (they stay on the slow cycle), and the tick runs only
     * while the activity is visible and is removed in {@link #onStop}, so it adds no wake lock and does
     * no work in the background, matching the existing tick behavior.
     */
    public void startDisplayedSessionCallScanTick() {
        if (mActivity.isVisible())
            refreshDisplayedSessionsForCallToUser();
        scheduleDisplayedSessionCallScanTick();
    }

    private void scheduleDisplayedSessionCallScanTick() {
        if (mDisplayedSessionCallScanTickScheduled) return;
        if (!mActivity.isVisible()) return;
        mDisplayedSessionCallScanTickScheduled = true;
        mMainThreadHandler.postDelayed(mDisplayedSessionCallScanTickRunnable,
            displayedSessionCallScanIntervalMillis());
    }

    private long displayedSessionCallScanIntervalMillis() {
        TermuxAppSharedProperties properties = mActivity.getProperties();
        int intervalMinutes = properties != null
            ? properties.getBackgroundDisplayedCallScanIntervalMinutes()
            : TermuxPropertyConstants.DEFAULT_IVALUE_BACKGROUND_DISPLAYED_CALL_SCAN_INTERVAL_MINUTES;
        return intervalMinutes * MILLIS_PER_MINUTE;
    }

    public void stopDisplayedSessionCallScanTick() {
        mDisplayedSessionCallScanTickScheduled = false;
        mMainThreadHandler.removeCallbacks(mDisplayedSessionCallScanTickRunnable);
    }

    private void onDisplayedSessionCallScanTick() {
        mDisplayedSessionCallScanTickScheduled = false;
        if (!mActivity.isVisible()) return;
        refreshDisplayedSessionsForCallToUser();
        scheduleDisplayedSessionCallScanTick();
    }

    private void refreshDisplayedSessionsForCallToUser() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        Set<String> displayedSessionNames = displayedSessionNames();
        if (displayedSessionNames.isEmpty()) return;

        reconnectDeadDisplayedSessionsInBackground(displayedSessionNames);

        List<StaggeredStatuslineRescanBatchPlanner.Batch> batches =
            StaggeredStatuslineRescanBatchPlanner.plan(new ArrayList<>(displayedSessionNames),
                STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE,
                SessionReconnectPacer.MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS);
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            StaggeredStatuslineRescanBatchPlanner.Batch batch = batches.get(batchIndex);
            Set<String> batchSessionNames = batch.getSessionNames();
            Runnable scanBatchOfDisplayedSessions = () -> {
                for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
                    TerminalSession session = termuxSession.getTerminalSession();
                    if (session == null || session.mSessionName == null) continue;
                    if (!batchSessionNames.contains(session.mSessionName)) continue;
                    backgroundOutputTagsForSession(session);
                }
            };
            if (batchIndex == 0) {
                scanBatchOfDisplayedSessions.run();
            } else {
                mMainThreadHandler.postDelayed(scanBatchOfDisplayedSessions, batch.getDelayMillis());
            }
        }
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession changedSession) {
        if (mActivity.getCurrentSession() == changedSession)
            updateBackgroundColor();
    }

    @Override
    public void onTerminalCursorStateChange(boolean enabled) {
        // Do not start cursor blinking thread if activity is not visible
        if (enabled && !mActivity.isVisible()) {
            Logger.logVerbose(LOG_TAG, "Ignoring call to start cursor blinking since activity is not visible");
            return;
        }

        // If cursor is to enabled now, then start cursor blinking if blinking is enabled
        // otherwise stop cursor blinking
        mActivity.getTerminalView().setTerminalCursorBlinkerState(enabled, false);
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        
        TermuxSession termuxSession = service.getTermuxSessionForTerminalSession(terminalSession);
        if (termuxSession != null)
            termuxSession.getExecutionCommand().mPid = pid;
    }



    @Override
    public Integer getTerminalCursorStyle() {
        return mActivity.getProperties().getTerminalCursorStyle();
    }



    /** Load mBellSoundPool */
    private synchronized void loadBellSoundPool() {
        if (mBellSoundPool == null) {
            mBellSoundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
                new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()).build();

            try {
                mBellSoundId = mBellSoundPool.load(mActivity, com.termux.shared.R.raw.bell, 1);
            } catch (Exception e){
                // Catch java.lang.RuntimeException: Unable to resume activity {com.termux/com.termux.app.TermuxActivity}: android.content.res.Resources$NotFoundException: File res/raw/bell.ogg from drawable resource ID
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load bell sound pool", e);
            }
        }
    }

    /** Release mBellSoundPool resources */
    private synchronized void releaseBellSoundPool() {
        if (mBellSoundPool != null) {
            mBellSoundPool.release();
            mBellSoundPool = null;
        }
    }



    /** Try switching to session. */
    public void setCurrentSession(TerminalSession session) {
        if (session == null) return;

        mStartupDisplayedSessionSelectionPending = false;

        stopActiveSessionSeenTick();

        TerminalSession previousSession = mActivity.getCurrentSession();
        boolean switchingSessions = previousSession != null && previousSession != session;

        if (switchingSessions) {
            TtsManager ttsManager = mActivity.getTtsManager();
            if (ttsManager != null) ttsManager.stop();
        }

        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter != null && switchingSessions)
            toolbarAdapter.saveTextInputForSession(previousSession);

        mActivity.getTerminalView().attachSession(session);

        startActiveSessionSeenTick();

        if (toolbarAdapter != null && switchingSessions)
            toolbarAdapter.restoreTextInputForSession(session);

        termuxSessionListNotifyUpdated();
        updateBackgroundColor();

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionChanged(session);

        mActivity.routePendingInboundBrowserUrl();

        SessionListBottomSheetController sessionListBottomSheetController = mActivity.getSessionListBottomSheetController();
        if (sessionListBottomSheetController != null)
            sessionListBottomSheetController.revealCurrentSessionRowIfShowing();

        openTagsForSession(session);
        backgroundOutputTagsForSession(session);

        enforceActiveSessionViewBinding(session);

        updateSessionNameOverlay();

        forceRemoteRepaintOnOpen(session);
    }

    private void forceRemoteRepaintOnOpen(@NonNull TerminalSession session) {
        TerminalView terminalView = mActivity.getTerminalView();
        if (terminalView == null) return;
        terminalView.post(() -> {
            if (mActivity.getCurrentSession() != session) return;
            TerminalEmulator emulator = session.getEmulator();
            boolean hasEmulator = emulator != null;
            int columns = hasEmulator ? emulator.mColumns : 0;
            int rows = hasEmulator ? emulator.mRows : 0;
            if (!SessionOpenRepaintDecision.shouldForceRemoteRepaint(session.isRunning(), hasEmulator, columns, rows))
                return;
            session.forceRemoteRepaint();
        });
    }

    /**
     * Switches to {@code session}, first reconnecting it in place when it is a dead definition-backed
     * session so the user always lands on a live session. This is the primary reconnect path for a
     * non-current stale session: instead of eagerly reconnecting every stale session in the background
     * (which churned the list and exhausted resources), a dead session is reconnected on demand only
     * when the user actually switches to it, taking roughly the ssh/tmux reattach time. When the
     * session is already live, or the reconnect cannot produce a replacement, the switch falls back to
     * the live session object so a tap is never lost.
     */
    public void switchToSessionReconnectingIfDead(@Nullable TerminalSession session) {
        if (session == null) return;
        unhideOpenedSession(session.mSessionName);
        if (shouldReconnectOnSwitch(session)) {
            TerminalSession reconnectedSession =
                reconnectDeadSessionPreservingDisplayedSession(session, true);
            if (reconnectedSession != null) {
                setCurrentSession(reconnectedSession);
                return;
            }
        }
        setCurrentSession(session);
    }

    private void unhideOpenedSession(@Nullable String sessionName) {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) return;
        OpenedSessionUnhider.unhideOpenedSession(preferences, sessionName);
    }

    private boolean shouldReconnectOnSwitch(@NonNull TerminalSession session) {
        if (session.isRunning()) return false;
        return decideFinishedSessionEnterAction(session).isReconnect();
    }

    private void enforceActiveSessionViewBinding(@NonNull TerminalSession session) {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();

        String activeSessionHandle = session.mHandle;
        TerminalSession displayedTerminalSession =
            (mActivity.getTerminalView() == null) ? null : mActivity.getTerminalView().getCurrentSession();
        String displayedTerminalSessionHandle =
            (displayedTerminalSession == null) ? null : displayedTerminalSession.mHandle;
        boolean browserVisible = browserController != null && browserController.isBrowserVisible();
        String displayedBrowserSessionHandle =
            (browserController == null) ? null : browserController.getDisplayedSessionHandle();
        boolean activeSessionHasBrowserTab =
            browserController != null && browserController.hasBrowserTabForSession(activeSessionHandle);

        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            activeSessionHandle, displayedTerminalSessionHandle, browserVisible,
            displayedBrowserSessionHandle, activeSessionHasBrowserTab);

        Logger.logDebug(LOG_TAG, resolution.diagnosticLine());

        if (resolution.requiresTerminalRebind())
            mActivity.getTerminalView().attachSession(session);

        if (resolution.requiresBrowserRebind() && browserController != null)
            browserController.reconcileDisplayedTabWithActiveSession(session);
    }

    public void updateSessionNameOverlay() {
        TextView sessionNameBar = mActivity.findViewById(R.id.session_name_bar);
        if (sessionNameBar == null) return;

        TerminalSession session = mActivity.getCurrentSession();
        String sessionName = (session == null) ? null : session.mSessionName;
        SessionRow currentSessionRow = currentSessionRow();
        if (!SessionNameBarVisibility.isVisible(sessionName)) {
            sessionNameBar.setText("");
            sessionNameBar.setVisibility(View.GONE);
            sessionNameBar.setOnClickListener(null);
            sessionNameBar.setClickable(false);
            updateSessionProjectStoryBar(null);
        } else {
            String title = currentSessionRow.getResolvedTitle();
            SessionNameBarContent content = SessionNameBarContent.of(sessionName, title);
            sessionNameBar.setSingleLine(false);
            sessionNameBar.setMaxLines(content.hasTitle() ? 3 : 2);
            sessionNameBar.setText(buildSessionNameBarText(content));
            sessionNameBar.setVisibility(View.VISIBLE);
            sessionNameBar.setOnClickListener(view -> onSessionNameBarTapped());
            updateSessionProjectStoryBar(currentSessionRow);
        }
        updateSessionInfoBottomBars(sessionName);
    }

    private void updateSessionInfoBottomBars(@Nullable String sessionName) {
        View sessionInfoRoot = mActivity.findViewById(android.R.id.content);
        if (sessionInfoRoot == null) return;
        SessionInfoBottomBarsBinder.bind(sessionInfoRoot, mActivity.getSessionNewActivityStore(),
            sessionName, System.currentTimeMillis());
    }

    @NonNull
    private SessionRow currentSessionRow() {
        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController == null) {
            return SessionRow.rowOrEmpty(null, -1);
        }
        return listViewController.getCurrentSessionRow();
    }

    private void updateSessionProjectStoryBar(@Nullable SessionRow currentSessionRow) {
        TextView projectStoryBar = mActivity.findViewById(R.id.session_project_story_bar);
        if (projectStoryBar == null) return;

        SessionProjectStoryLine line = (currentSessionRow == null)
            ? SessionProjectStoryLine.of(null, null)
            : SessionProjectStoryLine.of(currentSessionRow.getProject(), currentSessionRow.getStory());
        if (line.hasContent()) {
            projectStoryBar.setText(line.getText());
            projectStoryBar.setVisibility(View.VISIBLE);
        } else {
            projectStoryBar.setText("");
            projectStoryBar.setVisibility(View.GONE);
        }
    }

    private CharSequence buildSessionNameBarText(SessionNameBarContent content) {
        if (!content.hasTitle()) return content.getName();
        SpannableString spannable = new SpannableString(content.getText());
        applySessionNameBarTitleStyling(spannable, content.getTitleStart(), content.getTitleEnd());
        return spannable;
    }

    static void applySessionNameBarTitleStyling(@NonNull Spannable spannable, int titleStart, int titleEnd) {
        if (titleStart < 0 || titleEnd <= titleStart) return;
        spannable.setSpan(new RelativeSizeSpan(SESSION_NAME_BAR_TITLE_RELATIVE_SIZE),
            titleStart, titleEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(SESSION_NAME_BAR_TITLE_TEXT_COLOR),
            titleStart, titleEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void onSessionNameBarTapped() {
        TerminalSession session = mActivity.getCurrentSession();
        String sessionNameForCopy = (session == null) ? null
            : resolveSessionNameForCopy(session.mSessionName, session.getTitle());
        if (sessionNameForCopy == null || sessionNameForCopy.isEmpty()) return;

        ShareUtils.copyTextToClipboard(mActivity, sessionNameForCopy,
            mActivity.getString(R.string.msg_session_name_copied_to_clipboard));
    }

    public void copySessionNameToClipboard(TerminalSession session) {
        if (session == null) return;
        String textToCopy = resolveSessionNameForCopy(session.mSessionName, session.getTitle());
        if (TextUtils.isEmpty(textToCopy)) return;

        ShareUtils.copyTextToClipboard(mActivity, textToCopy,
            mActivity.getString(R.string.msg_session_name_copied_to_clipboard));
    }

    static String resolveSessionNameForCopy(String sessionName, String sessionTitle) {
        if (sessionName != null && !sessionName.isEmpty()) {
            return sessionName;
        }
        if (sessionTitle != null && !sessionTitle.isEmpty()) {
            return sessionTitle;
        }
        return null;
    }

    public void switchToSession(boolean forward) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        TerminalSession currentTerminalSession = mActivity.getCurrentSession();
        int currentIndex = service.getIndexOfSession(currentTerminalSession);
        int targetIndex = nextVisibleSessionIndexForSwitch(service, currentIndex, forward);

        TermuxSession termuxSession = service.getTermuxSession(targetIndex);
        if (termuxSession != null)
            setCurrentSession(termuxSession.getTerminalSession());
    }

    private int nextVisibleSessionIndexForSwitch(TermuxService service, int currentIndex, boolean forward) {
        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController != null) {
            int visibleIndex = listViewController.getNextVisibleSessionIndex(currentIndex, forward);
            if (visibleIndex >= 0) return visibleIndex;
        }
        return wrapAroundSessionIndex(currentIndex, service.getTermuxSessionsSize(), forward);
    }

    static int wrapAroundSessionIndex(int currentIndex, int size, boolean forward) {
        int index = currentIndex;
        if (forward) {
            if (++index >= size) index = 0;
        } else {
            if (--index < 0) index = size - 1;
        }
        return index;
    }

    public void switchToSession(int index) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        TermuxSession termuxSession = service.getTermuxSession(index);
        if (termuxSession != null)
            setCurrentSession(termuxSession.getTerminalSession());
    }

    @SuppressLint("InflateParams")
    public void renameSession(final TerminalSession sessionToRename) {
        if (sessionToRename == null) return;

        TextInputDialogUtils.textInput(mActivity, R.string.title_rename_session, sessionToRename.mSessionName, R.string.action_rename_session_confirm, text -> {
            renameSession(sessionToRename, text);
            termuxSessionListNotifyUpdated();
        }, -1, null, -1, null, null);
    }

    public void deleteSession(final TerminalSession sessionToDelete) {
        if (sessionToDelete == null) return;

        suppressReconnectForUserRemovedSession(sessionToDelete.mSessionName);
        finishAndRemoveSession(sessionToDelete);
    }

    /**
     * Killing the host session tears the local session down so that it can be established again, so
     * the session name is not recorded as removed when it is an always-present name.
     */
    private void deleteSessionAfterHostSessionKill(final TerminalSession sessionToDelete) {
        if (sessionToDelete == null) return;

        suppressReconnectAfterHostSessionKill(sessionToDelete.mSessionName);
        finishAndRemoveSession(sessionToDelete);
    }

    private void finishAndRemoveSession(final TerminalSession sessionToDelete) {
        TerminalSession currentSession = mActivity.getCurrentSession();
        sessionToDelete.finishIfRunning();
        removeFinishedSession(sessionToDelete);
        if (currentSession != null && currentSession != sessionToDelete)
            setCurrentSession(currentSession);
    }

    private void suppressReconnectForUserRemovedSession(@Nullable String sessionName) {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) return;
        if (mUserRemovedSessionReconnectSuppressionPlanner.shouldSuppressReconnectAfterUserRemoval(
                sessionName)) {
            preferences.setSessionUserRemoved(sessionName, true);
            preferences.recordSessionUserRemovedAt(sessionName, System.currentTimeMillis());
        }
    }

    private void suppressReconnectAfterHostSessionKill(@Nullable String sessionName) {
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) return;
        if (mUserRemovedSessionReconnectSuppressionPlanner.shouldSuppressReconnectAfterHostSessionKill(
                sessionName, preferences.getAlwaysNaSessionNames())) {
            preferences.setSessionUserRemoved(sessionName, true);
        }
    }

    public void killHostSession(final TerminalSession sessionToKill) {
        if (sessionToKill == null) return;

        HostTmuxSessionKiller.kill(new HostTmuxSessionKiller.Target() {
            @Override
            public boolean executeHostKillCommand(String commandSessionName, String hostKillCommand) {
                TermuxService service = mActivity.getTermuxService();
                if (service == null) return false;

                if (revealExistingSessionByName(commandSessionName, true)) return false;

                int liveSessionCount = cappedSessionCount(service);
                if (liveSessionCount >= maxSessions()) {
                    notifyMaxTerminalsReached(liveSessionCount);
                    return false;
                }

                String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
                String[] arguments = new String[]{"-c", hostKillCommand};
                return service.createTermuxSession(shellPath, arguments, null,
                    workingDirectoryForNewSession(), false, commandSessionName) != null;
            }

            @Override
            public void notifyUnavailable(KillHostSessionPlan.Outcome outcome) {
                mActivity.showToast(mActivity.getString(
                    outcome == KillHostSessionPlan.Outcome.SESSION_NAME_MISSING
                        ? R.string.msg_kill_host_session_name_missing
                        : R.string.msg_kill_session_command_not_configured), true);
            }

            @Override
            public void finishLocalSession() {
                deleteSessionAfterHostSessionKill(sessionToKill);
            }
        }, mActivity.getPreferences().getKillSessionCommand(), sessionToKill.mSessionName,
            mMainThreadHandler::postDelayed);
    }

    private String workingDirectoryForNewSession() {
        TerminalSession currentSession = mActivity.getCurrentSession();
        String currentSessionCwd = currentSession == null ? null : currentSession.getCwd();
        return currentSessionCwd == null || currentSessionCwd.isEmpty()
            ? mActivity.getProperties().getDefaultWorkingDirectory()
            : currentSessionCwd;
    }

    private void notifyMaxTerminalsReached(int liveSessionCount) {
        DiagnosticEventLogHolder.record(DiagnosticEventType.MAX_SESSIONS_REACHED,
            "cap=" + maxSessions());
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
            .setTitle(R.string.title_max_terminals_reached)
            .setMessage(mActivity.getString(R.string.msg_max_terminals_reached, liveSessionCount, maxSessions()))
            .setPositiveButton(android.R.string.ok, null));
    }

    public void resetHostSession(final TerminalSession sessionToReset) {
        if (sessionToReset == null) return;

        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        ResetSessionPlan plan = ResetSessionPlanner.plan(
            mActivity.getPreferences().getResetSessionCommand(), sessionToReset.mSessionName);
        if (!plan.shouldStart()) {
            showResetSessionUnavailableDialog(plan.getOutcome());
            return;
        }

        String resetSessionName = plan.getSessionName();
        if (revealExistingSessionByName(resetSessionName, true)) return;

        int liveSessionCount = cappedSessionCount(service);
        if (liveSessionCount >= maxSessions()) {
            DiagnosticEventLogHolder.record(DiagnosticEventType.MAX_SESSIONS_REACHED,
                "cap=" + maxSessions());
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
                .setTitle(R.string.title_max_terminals_reached)
                .setMessage(mActivity.getString(R.string.msg_max_terminals_reached, liveSessionCount, maxSessions()))
                .setPositiveButton(android.R.string.ok, null));
            return;
        }

        String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
        String[] arguments = new String[]{"-c", plan.getCommand()};
        TermuxSession resetTermuxSession = service.createTermuxSession(shellPath, arguments, null,
            mActivity.getProperties().getDefaultWorkingDirectory(), false, resetSessionName);
        if (resetTermuxSession == null) return;

        setCurrentSession(resetTermuxSession.getTerminalSession());
        mActivity.getDrawer().closeDrawers();
    }

    private void showResetSessionUnavailableDialog(@NonNull ResetSessionPlan.Outcome outcome) {
        int titleResId = outcome == ResetSessionPlan.Outcome.SESSION_NAME_MISSING
            ? R.string.title_reset_session_name_missing
            : R.string.title_reset_session_command_not_configured;
        int messageResId = outcome == ResetSessionPlan.Outcome.SESSION_NAME_MISSING
            ? R.string.msg_reset_session_name_missing
            : R.string.msg_reset_session_command_not_configured;
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
            .setTitle(titleResId)
            .setMessage(messageResId)
            .setPositiveButton(android.R.string.ok, null));
    }

    private void renameSession(TerminalSession sessionToRename, String text) {
        if (sessionToRename == null) return;
        sessionToRename.mSessionName = text;

        if (sessionToRename == mActivity.getCurrentSession())
            updateSessionNameOverlay();

        if (mPersistedSessionBySession.containsKey(sessionToRename))
            savePersistedSessions();
    }

    public void addNewSession(boolean isFailSafe, String sessionName) {
        addNewSession(isFailSafe, sessionName, true);
    }

    public void addNewSession(boolean isFailSafe, String sessionName, boolean closeDrawerAfter) {
        addNewSession(isFailSafe, sessionName, closeDrawerAfter, true);
    }

    public void addNewSession(boolean isFailSafe, String sessionName, boolean closeDrawerAfter,
                              boolean displayNewSession) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        if (revealExistingSessionByName(sessionName, closeDrawerAfter)) return;

        int liveSessionCount = cappedSessionCount(service);
        if (liveSessionCount >= maxSessions()) {
            DiagnosticEventLogHolder.record(DiagnosticEventType.MAX_SESSIONS_REACHED,
                "cap=" + maxSessions());
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity).setTitle(R.string.title_max_terminals_reached)
                .setMessage(mActivity.getString(R.string.msg_max_terminals_reached, liveSessionCount, maxSessions()))
                .setPositiveButton(android.R.string.ok, null));
        } else {
            TerminalSession currentSession = mActivity.getCurrentSession();

            String workingDirectory;
            if (currentSession == null) {
                workingDirectory = mActivity.getProperties().getDefaultWorkingDirectory();
            } else {
                workingDirectory = currentSession.getCwd();
            }

            TermuxSession newTermuxSession = service.createTermuxSession(null, null, null, workingDirectory, isFailSafe, sessionName);
            if (newTermuxSession == null) return;

            TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
            if (!isFailSafe)
                recordPersistedSession(newTerminalSession, new PersistedSession(newTerminalSession.mHandle, null, null, false, workingDirectory));
            attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
            if (displayNewSession) {
                setCurrentSession(newTerminalSession);
            } else {
                termuxSessionListNotifyUpdated();
            }

            if (closeDrawerAfter)
                mActivity.getDrawer().closeDrawers();
        }
    }

    public void addNewSessionApplyingAutosshConfig(String sessionName) {
        addNewSessionApplyingAutosshConfig(sessionName, true);
    }

    public void addNewSessionApplyingAutosshConfig(String sessionName, boolean closeDrawerAfter) {
        addNewSessionApplyingAutosshConfig(sessionName, closeDrawerAfter, true);
    }

    private void addNewSessionApplyingAutosshConfig(String sessionName, boolean closeDrawerAfter,
                                                    boolean displayNewSession) {
        String commandTemplate = mActivity.getPreferences().getAutosshCommand();
        SessionDefinitionPlannedSession plannedSession =
            new SessionDefinitionPlanner().planNamedSession(sessionName, commandTemplate);
        if (plannedSession.hasCommand()) {
            addNewAutosshSession(plannedSession.getName(), plannedSession.getCommand(), closeDrawerAfter,
                displayNewSession);
        } else {
            addNewSession(false, plannedSession.getName(), closeDrawerAfter, displayNewSession);
        }
    }

    public void addNewSessionForBrowserUrl(String sessionName) {
        addNewSessionForBrowserUrl(sessionName, true);
    }

    public void addNewSessionForBrowserUrl(String sessionName, boolean closeDrawerAfter) {
        addNewSessionApplyingAutosshConfig(sessionName, closeDrawerAfter);
    }

    public void addNewAutosshSession(String sessionName, String command) {
        addNewAutosshSession(sessionName, command, true);
    }

    public void addNewAutosshSession(String sessionName, String command, boolean closeDrawerAfter) {
        addNewAutosshSession(sessionName, command, closeDrawerAfter, true);
    }

    private void addNewAutosshSession(String sessionName, String command, boolean closeDrawerAfter,
                                      boolean displayNewSession) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        if (revealExistingSessionByName(sessionName, closeDrawerAfter)) return;

        int liveSessionCount = cappedSessionCount(service);
        if (liveSessionCount >= maxSessions()) {
            DiagnosticEventLogHolder.record(DiagnosticEventType.MAX_SESSIONS_REACHED,
                "cap=" + maxSessions());
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity).setTitle(R.string.title_max_terminals_reached)
                .setMessage(mActivity.getString(R.string.msg_max_terminals_reached, liveSessionCount, maxSessions()))
                .setPositiveButton(android.R.string.ok, null));
        } else {
            TerminalSession currentSession = mActivity.getCurrentSession();

            String workingDirectory;
            if (currentSession == null) {
                workingDirectory = mActivity.getProperties().getDefaultWorkingDirectory();
            } else {
                workingDirectory = currentSession.getCwd();
            }

            String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
            String[] arguments = new String[]{"-c", command};
            TermuxSession newTermuxSession = service.createTermuxSession(shellPath, arguments, null, workingDirectory, false, sessionName);
            if (newTermuxSession == null) return;

            TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
            recordPersistedSession(newTerminalSession, new PersistedSession(newTerminalSession.mHandle, shellPath, arguments, false, workingDirectory));
            attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
            if (displayNewSession) {
                setCurrentSession(newTerminalSession);
            } else {
                termuxSessionListNotifyUpdated();
            }

            if (closeDrawerAfter)
                mActivity.getDrawer().closeDrawers();
        }
    }

    public void recreateUnhiddenSessionWithoutDisplacingTheDisplayedSession(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        if (service.getTermuxSessionForSessionName(sessionName) != null) return;

        addNewSessionApplyingAutosshConfig(sessionName, false, false);
        mActivity.eagerLoadAllSessions();
    }

    public void releaseHiddenSessionRuntimeResources(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        TermuxSession hiddenTermuxSession = service.getTermuxSessionForSessionName(sessionName);
        if (hiddenTermuxSession == null) return;
        TerminalSession hiddenSession = hiddenTermuxSession.getTerminalSession();
        if (hiddenSession == null) return;

        boolean hiddenSessionIsDisplayed = mActivity.getCurrentSession() == hiddenSession;
        TerminalSession sessionToRenderInstead = hiddenSessionIsDisplayed
            ? topmostSessionTheOwnerCanSee(sessionName)
            : null;
        if (hiddenSessionIsDisplayed && sessionToRenderInstead == null) {
            return;
        }

        cancelReconnectTimeout(sessionName);

        purgeNewActivityForRemovedSession(sessionName);

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionRemoved(hiddenSession);

        if (mPersistedSessionBySession.remove(hiddenSession) != null)
            savePersistedSessions();

        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter != null)
            toolbarAdapter.removeTextInputForSession(hiddenSession);

        hiddenSession.releaseRuntimeResources();
        service.removeTermuxSession(hiddenSession);

        if (sessionToRenderInstead != null)
            setCurrentSession(sessionToRenderInstead);

        termuxSessionListNotifyUpdated();
    }

    public boolean hidingLeavesTheViewOnASessionThatStillWorks(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return true;
        TerminalSession currentSession = mActivity.getCurrentSession();
        if (currentSession == null || !sessionName.equals(currentSession.mSessionName)) return true;
        return topmostSessionTheOwnerCanSee(sessionName) != null;
    }

    public boolean hidingCanReleaseWhateverIsLiveForTheName(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) return false;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;
        TermuxSession liveTermuxSession = service.getTermuxSessionForSessionName(sessionName);
        return liveTermuxSession == null || liveTermuxSession.getTerminalSession() != null;
    }

    private boolean revealExistingSessionByName(@Nullable String sessionName, boolean closeDrawerAfter) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;

        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }

        DuplicateSessionNameResolution resolution = DuplicateSessionNameResolver.resolve(
            sessionName, liveSessionNames, mActivity.getPreferences().getDisabledSessionNames());
        if (!resolution.shouldRevealExisting()) return false;

        String existingSessionName = resolution.getExistingSessionName();
        TerminalSession existingTerminalSession = null;
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            if (existingSessionName.equals(termuxSession.getTerminalSession().mSessionName)) {
                existingTerminalSession = termuxSession.getTerminalSession();
                break;
            }
        }
        if (existingTerminalSession == null) return false;

        if (resolution.requiresUnhide())
            mActivity.getPreferences().setSessionDisabled(existingSessionName, false);
        setCurrentSession(existingTerminalSession);

        if (closeDrawerAfter)
            mActivity.getDrawer().closeDrawers();

        return true;
    }

    private void restoreBrowserTabsForReconnectedSession(@NonNull TerminalSession session, @Nullable String sessionName) {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) return;
        browserController.restoreTabsForReconnectedSession(session.mHandle, sessionName);
    }

    private void attachBrowserTabForUrlSessionName(@NonNull TerminalSession session, @Nullable String sessionName) {
        String browserTabUrl = mSessionNameBrowserTabUrlResolver.resolve(sessionName);
        if (browserTabUrl == null) return;
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) return;
        browserController.attachBackgroundTab(session.mHandle, browserTabUrl);
    }

    /**
     * Switch to the stored-or-last session only when no valid session is already displayed. Used on
     * service reconnect so an activity rebinding to a service with running sessions does not yank the
     * user away from the session they are actively working in.
     */
    public void setCurrentSessionOnReconnectIfNoneDisplayed() {
        if (shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession()))
            setStartupDisplayedSession(getCurrentStoredSessionOrLast());
    }

    @Nullable
    private TerminalSession startupDisplayedSession(@Nullable TerminalSession fallbackSession) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return fallbackSession;
        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController == null) return fallbackSession;
        int fallbackSessionIndex = fallbackSession == null ? -1 : service.getIndexOfSession(fallbackSession);
        int sessionIndexToDisplay = StartupDisplayedSessionDecision.sessionIndexToDisplay(
            listViewController.getTopmostNonHiddenSessionIndex(), fallbackSessionIndex);
        if (sessionIndexToDisplay < 0) return fallbackSession;
        List<TermuxSession> termuxSessions = service.getTermuxSessions();
        if (sessionIndexToDisplay >= termuxSessions.size()) return fallbackSession;
        TerminalSession sessionToDisplay = termuxSessions.get(sessionIndexToDisplay).getTerminalSession();
        return sessionToDisplay == null ? fallbackSession : sessionToDisplay;
    }

    private void setStartupDisplayedSession(@Nullable TerminalSession fallbackSession) {
        setCurrentSession(startupDisplayedSession(fallbackSession));
        mStartupDisplayedSessionSelectionPending = true;
    }

    public void reapplyStartupDisplayedSessionAfterEntriesLoaded(boolean sessionDefinitionEntriesLoaded) {
        if (!mStartupDisplayedSessionSelectionPending) return;
        TerminalSession currentSession = mActivity.getCurrentSession();
        TerminalSession sessionToDisplay = startupDisplayedSession(currentSession);
        mStartupDisplayedSessionSelectionPending = !sessionDefinitionEntriesLoaded;
        if (sessionToDisplay == null || sessionToDisplay == currentSession) return;
        setCurrentSession(sessionToDisplay);
    }

    /**
     * Restore the session that was displayed before a reload to the displayed session if it is still
     * a live session. A session-definition reload creates and removes sessions, and each creation
     * switches the displayed session; restoring the pre-reload session afterwards keeps the user on
     * the session they were working in instead of being yanked to the last reloaded session.
     */
    public void restoreDisplayedSessionAfterReloadIfStillLive(@Nullable TerminalSession sessionBeforeReload) {
        if (sessionBeforeReload == null) return;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;
        if (service.getIndexOfSession(sessionBeforeReload) < 0) return;
        if (mActivity.getCurrentSession() == sessionBeforeReload) return;
        setCurrentSession(sessionBeforeReload);
    }

    public void setCurrentStoredSession() {
        TerminalSession currentSession = mActivity.getCurrentSession();
        if (currentSession != null)
            mActivity.getPreferences().setCurrentSession(currentSession.mHandle);
        else
            mActivity.getPreferences().setCurrentSession(null);
    }

    /** The current session as stored or the last one if that does not exist. */
    public TerminalSession getCurrentStoredSessionOrLast() {
        TerminalSession stored = getCurrentStoredSession();

        if (stored != null) {
            // If a stored session is in the list of currently running sessions, then return it
            return stored;
        } else {
            // Else return the last session currently running
            TermuxService service = mActivity.getTermuxService();
            if (service == null) return null;

            TermuxSession termuxSession = service.getLastTermuxSession();
            if (termuxSession != null)
                return termuxSession.getTerminalSession();
            else
                return null;
        }
    }

    private TerminalSession getCurrentStoredSession() {
        String sessionHandle = mActivity.getPreferences().getCurrentSession();

        // If no session is stored in shared preferences
        if (sessionHandle == null)
            return null;

        // Check if the session handle found matches one of the currently running sessions
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;

        return service.getTerminalSessionForHandle(sessionHandle);
    }

    public void removeFinishedSession(TerminalSession finishedSession) {
        // Return pressed with finished session - remove it.
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        if (finishedSession != null)
            purgeNewActivityForRemovedSession(finishedSession.mSessionName);

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionRemoved(finishedSession);

        if (mPersistedSessionBySession.remove(finishedSession) != null)
            savePersistedSessions();

        TerminalSession currentSession = mActivity.getCurrentSession();
        boolean finishedSessionWasCurrent = currentSession != null && currentSession == finishedSession;

        TermuxSession neighborOfFinishedSession = resolveNeighborSessionBeforeRemoval(finishedSession);

        service.removeTermuxSession(finishedSession);

        int size = service.getTermuxSessionsSize();
        if (size == 0) {
            // There are no sessions to show, so finish the activity.
            mActivity.finishActivityIfNotFinishing();
        } else {
            boolean currentSessionStillPresent = currentSession != null && service.getIndexOfSession(currentSession) >= 0;
            if (shouldReselectCurrentSessionAfterRemoval(finishedSessionWasCurrent, currentSessionStillPresent)) {
                TermuxSession nextSession = neighborStillPresent(service, neighborOfFinishedSession)
                    ? neighborOfFinishedSession
                    : selectNextVisibleSession();
                if (nextSession != null)
                    setCurrentSession(nextSession.getTerminalSession());
            } else {
                termuxSessionListNotifyUpdated();
            }
        }

        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter != null)
            toolbarAdapter.removeTextInputForSession(finishedSession);
    }

    @NonNull
    public FinishedSessionEnterAction decideFinishedSessionEnterAction(@Nullable TerminalSession finishedSession) {
        if (finishedSession == null) {
            return FinishedSessionEnterAction.decide(null, null);
        }
        return FinishedSessionEnterAction.decide(finishedSession.mSessionName,
            mActivity.getPreferences().getAutosshCommand(),
            mActivity.getPreferences().getUserRemovedSessionNames());
    }

    public boolean reconnectFinishedSessionInPlace(@Nullable TerminalSession finishedSession,
                                                   @Nullable String pendingInput) {
        if (finishedSession == null) return false;

        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;

        if (isRecordedHidden(finishedSession.mSessionName)) return false;

        FinishedSessionEnterAction action = decideFinishedSessionEnterAction(finishedSession);
        if (!action.isReconnect()) return false;

        String sessionName = action.getSessionName();
        if (isRecordedHidden(sessionName)) return false;
        String command = action.getCommand();
        TerminalSession currentSession = mActivity.getCurrentSession();
        String currentSessionCwd = currentSession == null ? null : currentSession.getCwd();
        String workingDirectory = currentSessionCwd == null || currentSessionCwd.isEmpty()
            ? mActivity.getProperties().getDefaultWorkingDirectory()
            : currentSessionCwd;
        String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
        String[] arguments = new String[]{"-c", command};

        purgeNewActivityForReconnectedSession(finishedSession.mSessionName);

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionRemoved(
                finishedSession, BrowserSessionRemovalReason.RECONNECT);

        if (mPersistedSessionBySession.remove(finishedSession) != null)
            savePersistedSessions();

        dropToolbarTextInputForSession(finishedSession);

        service.removeTermuxSession(finishedSession);

        TermuxSession newTermuxSession =
            service.createTermuxSession(shellPath, arguments, null, workingDirectory, false, sessionName);
        if (newTermuxSession == null) {
            Logger.logError(LOG_TAG, "Failed to reconnect session \"" + sessionName
                + "\" in place; live session count delta "
                + netLiveSessionCountDeltaForReconnect(false));
            termuxSessionListNotifyUpdated();
            return false;
        }

        TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
        recordPersistedSession(newTerminalSession,
            new PersistedSession(newTerminalSession.mHandle, shellPath, arguments, false, workingDirectory));
        restoreBrowserTabsForReconnectedSession(newTerminalSession, sessionName);
        attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
        setCurrentSession(newTerminalSession);
        dropToolbarTextInputForSession(finishedSession);

        replayPendingInputWhenConnected(newTerminalSession, pendingInput);
        return true;
    }

    private void dropToolbarTextInputForSession(@Nullable TerminalSession session) {
        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter == null) return;
        toolbarAdapter.removeTextInputForSession(session);
    }

    /**
     * The net change to the live session count produced by a single reconnect, which removes the dead
     * session and then creates its replacement. When the replacement is created the delta is zero (one
     * removed, one added); when the replacement fails the delta is negative one (one removed, none
     * added). A reconnect therefore never increases the live session count, so a failed reconnect can
     * never accumulate live sessions toward the maximum-sessions cap and can never leave a duplicate.
     */
    static int netLiveSessionCountDeltaForReconnect(boolean replacementCreated) {
        return replacementCreated ? 0 : -1;
    }

    static boolean liveSessionWithNameAlreadyExists(@Nullable String sessionName,
                                                    @NonNull Collection<String> currentLiveSessionNames) {
        if (sessionName == null || sessionName.isEmpty()) return false;
        return currentLiveSessionNames.contains(sessionName);
    }

    private void replayPendingInputWhenConnected(@NonNull TerminalSession reconnectedSession,
                                                 @Nullable String pendingInput) {
        if (!ReconnectedSessionInputReplayPlanner.hasReplayableInput(pendingInput)) return;
        mMainThreadHandler.post(new ReconnectedSessionInputReplay(mMainThreadHandler, reconnectedSession,
            ReconnectedSessionInputReplayPlanner.replayPayload(pendingInput)));
    }

    @NonNull
    public List<String> reconnectDeadDefinitionBackedSessionsInBackground() {
        return reconnectDeadDefinitionBackedSessionsInBackground(visibleSessionNames());
    }

    private List<String> reconnectDeadDisplayedSessionsInBackground(@NonNull Set<String> displayedSessionNames) {
        return reconnectDeadDefinitionBackedSessionsInBackground(displayedSessionNames);
    }

    private List<String> reconnectDeadDefinitionBackedSessionsInBackground(@NonNull Set<String> reconnectableSessionNames) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return Collections.emptyList();

        releaseRuntimeResourcesOfSessionsThatMustHoldNone();

        String autosshCommandTemplate = mActivity.getPreferences().getAutosshCommand();
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        String currentSessionName = activeSessionName();
        long nowMillis = System.currentTimeMillis();

        Map<String, TerminalSession> sessionByName = new HashMap<>();
        List<DeadSessionReconnectPlanner.CandidateSession> candidateSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) continue;
            String sessionName = terminalSession.mSessionName;
            if (sessionName == null) continue;
            if (!reconnectableSessionNames.contains(sessionName)) continue;
            sessionByName.put(sessionName, terminalSession);
            boolean current = sessionName.equals(currentSessionName);
            boolean running = terminalSession.isRunning();
            Long lastOutTimeMillis = store == null ? null : store.getStatuslineOutTimeMillis(sessionName);
            boolean hung = mHungSessionDetector.isHung(lastOutTimeMillis, nowMillis);
            boolean reconnecting = store != null && store.isReconnecting(sessionName);
            boolean unableToReceiveInputLongEnough =
                mSessionInputDeliverabilityDwell.hasBeenUnableToReceiveInputLongEnough(
                    sessionName, terminalSession.inputReachesTheProgramReadingTheTerminal(), nowMillis);
            candidateSessions.add(new DeadSessionReconnectPlanner.CandidateSession(
                sessionName, running, current, hung, lastOutTimeMillis, reconnecting,
                unableToReceiveInputLongEnough));
        }

        List<String> sessionNamesToReconnect =
            mDeadSessionReconnectPlanner.planSessionNamesToReconnect(candidateSessions, autosshCommandTemplate,
                DeadSessionReconnectPlanner.UNLIMITED,
                mActivity.getPreferences().getUserRemovedSessionNames());
        mSessionNameToMaterializeOnTheNextBulkReconnect =
            oldestPendingCallToUserSessionName(store, sessionNamesToReconnect);
        List<String> reconnectedSessionNames = new ArrayList<>();
        for (String sessionName : sessionNamesToReconnect) {
            TerminalSession deadSession = sessionByName.get(sessionName);
            if (deadSession == null) {
                continue;
            }
            mSessionReconnectPacer.enqueueSession(deadSession);
            mSessionInputDeliverabilityDwell.forget(sessionName);
            reconnectedSessionNames.add(sessionName);
        }
        return reconnectedSessionNames;
    }

    @Nullable
    private String oldestPendingCallToUserSessionName(@Nullable SessionNewActivityStore store,
                                                      @NonNull List<String> sessionNames) {
        if (store == null) return null;
        String oldestPendingCallSessionName = null;
        long oldestPendingCallTimeMillis = Long.MAX_VALUE;
        for (String sessionName : sessionNames) {
            Long pendingCallSinceTimeMillis = store.pendingCallToUserSinceTimeMillis(sessionName);
            if (pendingCallSinceTimeMillis == null) continue;
            if (pendingCallSinceTimeMillis >= oldestPendingCallTimeMillis) continue;
            oldestPendingCallTimeMillis = pendingCallSinceTimeMillis;
            oldestPendingCallSessionName = sessionName;
        }
        return oldestPendingCallSessionName;
    }

    private void releaseRuntimeResourcesOfSessionsThatMustHoldNone() {
        if (!mActivity.isVisible()) return;

        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        String currentSessionName = activeSessionName();
        Set<String> displayedSessionNames = displayedSessionNames();
        Set<String> hiddenSessionNames = hiddenSessionNames();

        Map<String, TerminalSession> sessionByName = new HashMap<>();
        List<SessionResourceReleasePlanner.CandidateSession> candidateSessions = new ArrayList<>();
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) continue;
            String sessionName = terminalSession.mSessionName;
            if (sessionName == null) continue;
            sessionByName.put(sessionName, terminalSession);
            candidateSessions.add(new SessionResourceReleasePlanner.CandidateSession(sessionName,
                terminalSession.isRunning(), sessionName.equals(currentSessionName),
                displayedSessionNames.contains(sessionName), hiddenSessionNames.contains(sessionName)));
        }

        for (String sessionName : mSessionResourceReleasePlanner.planSessionNamesToRelease(candidateSessions)) {
            if (hiddenSessionNames.contains(sessionName)) {
                releaseHiddenSessionRuntimeResources(sessionName);
                continue;
            }
            releaseSessionRuntimeResources(sessionByName.get(sessionName), sessionName);
        }
    }

    private void releaseSessionRuntimeResources(@Nullable TerminalSession terminalSession,
                                                @NonNull String sessionName) {
        if (terminalSession == null) return;
        if (terminalSession == mActivity.getCurrentSession()) return;
        if (!terminalSession.isRunning() && terminalSession.getEmulator() == null) return;
        cancelReconnectTimeout(sessionName);
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store != null) {
            store.clearReconnecting(sessionName);
        }
        terminalSession.releaseRuntimeResourcesKeepingTheRowReopenable();
        mSessionOutputProgressTracker.forget(sessionName);
        termuxSessionListNotifyUpdated();
    }

    @Nullable
    private TerminalSession topmostSessionTheOwnerCanSee(@Nullable String sessionNameToLeaveOut) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;

        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController == null) return null;

        TerminalSession topmostSessionHoldingNoRuntime = null;
        for (int sessionIndex : listViewController.getSessionIndexesOfRowsTheOwnerCanSee()) {
            TermuxSession termuxSession = service.getTermuxSession(sessionIndex);
            if (termuxSession == null) continue;

            TerminalSession terminalSession = termuxSession.getTerminalSession();
            if (terminalSession == null) continue;
            if (terminalSession.mSessionName != null
                && terminalSession.mSessionName.equals(sessionNameToLeaveOut)) continue;

            if (terminalSession.getEmulator() != null || terminalSession.isRunning()) {
                return terminalSession;
            }
            if (topmostSessionHoldingNoRuntime == null) {
                topmostSessionHoldingNoRuntime = terminalSession;
            }
        }
        return topmostSessionHoldingNoRuntime;
    }

    private boolean isSessionStillInTheReconnectList(@NonNull TerminalSession session) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;
        boolean stillLive = false;
        for (TermuxSession termuxSession : new ArrayList<>(service.getTermuxSessions())) {
            if (termuxSession.getTerminalSession() == session) {
                stillLive = true;
                break;
            }
        }
        if (!stillLive) return false;
        String sessionName = session.mSessionName;
        if (sessionName == null) return false;
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) return true;
        if (preferences.getUserRemovedSessionNames().contains(sessionName)) return false;
        return !HiddenSessionNameMatcher.matchesAHiddenSession(sessionName,
            preferences.getDisabledSessionNames());
    }

    private void reconnectThenMarkSessionReconnecting(@NonNull TerminalSession deadSession) {
        String deadSessionName = deadSession.mSessionName;
        boolean materializeThisReplacement = deadSessionName != null
            && deadSessionName.equals(mSessionNameToMaterializeOnTheNextBulkReconnect);
        TerminalSession reconnectedSession =
            reconnectDeadSessionPreservingDisplayedSession(deadSession, materializeThisReplacement);
        if (reconnectedSession == null) return;
        markSessionReconnecting(deadSessionName, false);
    }

    /**
     * The on-demand "refresh everything to the current state" action shared by the reload / Load
     * Sessions button press and the periodic foreground reconnect tick. It reconnects every dead /
     * finished definition-backed session (healthy live sessions are left untouched by {@link
     * DeadSessionReconnectPlanner}), then force-refreshes the latest call/out/reply statusline state for
     * every session so each one lands on its correct tier instead of the uncolored tier. A
     * freshly-reconnected session has no rendered emulator at the instant it is created, so its
     * statusline cannot be read yet; the rescan is therefore run immediately for the sessions that are
     * already live and then re-posted on a bounded backoff schedule ({@link
     * #POST_RECONNECT_STATUSLINE_RESCAN_BACKOFF_MILLIS}, beginning at {@link
     * #ON_LOAD_STATUSLINE_RESCAN_DELAY_MILLIS}) so a session whose ssh/tmux statusline renders later than
     * the first delay is still picked up. The retry stops early once every just-reconnected session has a
     * parsed statusline recorded, and is capped at the end of the backoff schedule so it can never run
     * unbounded. The reschedule is lightweight main-thread {@link Handler#postDelayed} only; each rescan
     * goes through the forced {@link #repopulateStatuslineTimesForAllSessions(boolean)} path, which
     * bypasses the content-version skip-gate while keeping the heavy transcript read and parse off the
     * main thread via {@link #parseAndApplyStatuslineUpdatesOffThread}.
     */
    public void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {
        List<String> reconnectedSessionNames = reconnectDeadDefinitionBackedSessionsInBackground();
        repopulateStatuslineTimesForAllSessions(true);
        // Beyond the narrow visible set, refresh every displayed (non-hidden) row so the reload press
        // updates each session's content without a manual Send per session.
        repopulateStatuslineTimesForDisplayedSessions(true);
        if (reconnectedSessionNames.isEmpty()) return;
        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController != null) {
            listViewController.beginPostReconnectRescanWindow();
        }
        mMainThreadHandler.postDelayed(
            new PostReconnectStatuslineRescanRetry(reconnectedSessionNames),
            mPostReconnectStatuslineRescanRetryPlanner.firstAttemptDelayMillis());
    }

    private boolean allReconnectedSessionsHaveParsedStatusline(@NonNull List<String> reconnectedSessionNames) {
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return false;
        for (String sessionName : reconnectedSessionNames) {
            if (!hasParsedStatusline(store, sessionName)) return false;
        }
        return true;
    }

    private static boolean hasParsedStatusline(@NonNull SessionNewActivityStore store,
                                               @NonNull String sessionName) {
        return store.getStatuslineCallTimeMillis(sessionName) != null
            || store.getStatuslineOutTimeMillis(sessionName) != null
            || store.getStatuslineReplyTimeMillis(sessionName) != null
            || store.getSubagentCount(sessionName) > 0;
    }

    private final class PostReconnectStatuslineRescanRetry implements Runnable {

        @NonNull
        private final List<String> mReconnectedSessionNames;

        private int mNextBackoffIndex = 1;

        private PostReconnectStatuslineRescanRetry(@NonNull List<String> reconnectedSessionNames) {
            mReconnectedSessionNames = reconnectedSessionNames;
        }

        @Override
        public void run() {
            repopulateStatuslineTimesForAllSessions(true);
            boolean allParsed = allReconnectedSessionsHaveParsedStatusline(mReconnectedSessionNames);
            if (!mPostReconnectStatuslineRescanRetryPlanner.shouldScheduleNextAttempt(mNextBackoffIndex, allParsed)) {
                return;
            }
            long delayMillis =
                mPostReconnectStatuslineRescanRetryPlanner.delayUntilNextAttemptMillis(mNextBackoffIndex);
            mNextBackoffIndex++;
            mMainThreadHandler.postDelayed(this, delayMillis);
        }
    }

    @Nullable
    private TerminalSession reconnectDeadSessionPreservingDisplayedSession(
        @NonNull TerminalSession deadSession, boolean eagerlyInitializeReplacement) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;

        if (isRecordedHidden(deadSession.mSessionName)) return null;

        FinishedSessionEnterAction action = decideFinishedSessionEnterAction(deadSession);
        if (!action.isReconnect()) return null;

        String sessionName = action.getSessionName();
        if (isRecordedHidden(sessionName)) return null;
        String command = action.getCommand();
        TerminalSession displayedSession = mActivity.getCurrentSession();
        String deadSessionCwd = deadSession.getCwd();
        String workingDirectory = deadSessionCwd == null || deadSessionCwd.isEmpty()
            ? mActivity.getProperties().getDefaultWorkingDirectory()
            : deadSessionCwd;
        String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
        String[] arguments = new String[]{"-c", command};

        purgeNewActivityForReconnectedSession(deadSession.mSessionName);

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionRemoved(
                deadSession, BrowserSessionRemovalReason.RECONNECT);

        if (mPersistedSessionBySession.remove(deadSession) != null)
            savePersistedSessions();

        dropToolbarTextInputForSession(deadSession);

        service.removeTermuxSessionBeingReplaced(deadSession);

        List<String> currentLiveSessionNames = new ArrayList<>();
        for (TermuxSession liveSession : service.getTermuxSessions()) {
            currentLiveSessionNames.add(liveSession.getTerminalSession().mSessionName);
        }
        if (liveSessionWithNameAlreadyExists(sessionName, currentLiveSessionNames)) {
            return null;
        }

        TermuxSession newTermuxSession =
            service.createTermuxSession(shellPath, arguments, null, workingDirectory, false, sessionName);
        if (newTermuxSession == null) {
            DiagnosticEventLogHolder.record(DiagnosticEventType.RECONNECT_FAILED,
                sessionName == null ? "" : sessionName);
            Logger.logError(LOG_TAG, "Failed to reconnect dead session \"" + sessionName
                + "\"; the replacement session could not be created; live session count delta "
                + netLiveSessionCountDeltaForReconnect(false));
            termuxSessionListNotifyUpdated();
            return null;
        }

        DiagnosticEventLogHolder.record(DiagnosticEventType.SESSION_RECONNECTED,
            sessionName == null ? "" : sessionName);

        TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
        recordPersistedSession(newTerminalSession,
            new PersistedSession(newTerminalSession.mHandle, shellPath, arguments, false, workingDirectory));
        restoreBrowserTabsForReconnectedSession(newTerminalSession, sessionName);
        attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);

        if (displayedSession == deadSession) {
            carryUnsubmittedToolbarInputToReconnectedSession(newTerminalSession);
            setCurrentSession(newTerminalSession);
            dropToolbarTextInputForSession(deadSession);
        } else {
            if (eagerlyInitializeReplacement) {
                eagerInitializeReconnectedBackgroundSessionEmulator(newTerminalSession);
            }
            termuxSessionListNotifyUpdated();
        }
        return newTerminalSession;
    }

    private void carryUnsubmittedToolbarInputToReconnectedSession(@NonNull TerminalSession reconnectedSession) {
        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter == null) return;
        toolbarAdapter.saveTextInputForSession(reconnectedSession);
    }

    private void eagerInitializeReconnectedBackgroundSessionEmulator(@NonNull TerminalSession session) {
        if (session.getEmulator() != null) return;
        TerminalView terminalView = mActivity.getTerminalView();
        if (terminalView == null) return;
        int[] dimensions = terminalView.computeSessionEmulatorDimensions();
        if (dimensions == null) return;
        session.updateSize(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
        session.forceRemoteRepaint();
    }

    private static final class ReconnectedSessionInputReplay implements Runnable {

        @NonNull
        private final Handler mainThreadHandler;

        @NonNull
        private final TerminalSession reconnectedSession;

        @NonNull
        private final String textToSend;

        private int remainingAttempts = ReconnectedSessionInputReplayPlanner.MAX_RETRY_ATTEMPTS;

        private ReconnectedSessionInputReplay(@NonNull Handler mainThreadHandler,
                                              @NonNull TerminalSession reconnectedSession,
                                              @NonNull String textToSend) {
            this.mainThreadHandler = mainThreadHandler;
            this.reconnectedSession = reconnectedSession;
            this.textToSend = textToSend;
        }

        @Override
        public void run() {
            if (reconnectedSession.isRunning()) {
                reconnectedSession.write(textToSend);
                return;
            }
            remainingAttempts--;
            if (!ReconnectedSessionInputReplayPlanner.shouldScheduleAnotherAttempt(remainingAttempts)) {
                return;
            }
            mainThreadHandler.postDelayed(this, ReconnectedSessionInputReplayPlanner.RETRY_DELAY_MILLIS);
        }
    }

    public void removeSessionForRebuild(TerminalSession sessionToRemove) {
        if (sessionToRemove == null) return;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        purgeNewActivityForRemovedSession(sessionToRemove.mSessionName);

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionRemoved(sessionToRemove);

        if (mPersistedSessionBySession.remove(sessionToRemove) != null)
            savePersistedSessions();

        sessionToRemove.finishIfRunning();
        service.removeTermuxSession(sessionToRemove);

        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter != null)
            toolbarAdapter.removeTextInputForSession(sessionToRemove);
    }

    static boolean shouldReselectCurrentSessionAfterRemoval(boolean finishedSessionWasCurrent, boolean currentSessionStillPresent) {
        return finishedSessionWasCurrent || !currentSessionStillPresent;
    }

    @Nullable
    private TermuxSession selectNextVisibleSession() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;

        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController == null) return service.getTermuxSession(service.getTermuxSessionsSize() - 1);

        int firstVisibleSessionIndex = listViewController.getFirstVisibleSessionIndexAfterRebuild();
        if (firstVisibleSessionIndex < 0) return service.getTermuxSession(service.getTermuxSessionsSize() - 1);

        return service.getTermuxSession(firstVisibleSessionIndex);
    }

    @Nullable
    private TermuxSession resolveNeighborSessionBeforeRemoval(@Nullable TerminalSession sessionBeingRemoved) {
        if (sessionBeingRemoved == null) return null;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;

        TermuxSessionsListViewController listViewController = mActivity.getTermuxSessionListViewController();
        if (listViewController == null) return null;

        int removedSessionIndex = service.getIndexOfSession(sessionBeingRemoved);
        if (removedSessionIndex < 0) return null;

        List<Integer> orderedSessionIndexes = listViewController.getOrderedSessionIndexes();
        int killedVisiblePosition = orderedSessionIndexes.indexOf(removedSessionIndex);

        int neighborPosition = NextVisibleSessionAfterKillSelector.selectNextVisibleSessionPosition(
            orderedSessionIndexes.size(), killedVisiblePosition);
        if (neighborPosition < 0 || neighborPosition >= orderedSessionIndexes.size()) return null;

        return service.getTermuxSession(orderedSessionIndexes.get(neighborPosition));
    }

    private static boolean neighborStillPresent(@NonNull TermuxService service, @Nullable TermuxSession neighborSession) {
        return neighborSession != null
            && service.getIndexOfSession(neighborSession.getTerminalSession()) >= 0;
    }

    public void ensureCurrentSessionValidAfterRebuild() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        TerminalSession currentSession = mActivity.getCurrentSession();
        boolean currentSessionStillPresent = currentSession != null && service.getIndexOfSession(currentSession) >= 0;
        if (!shouldSwitchSessionAfterRebuild(currentSessionStillPresent)) return;

        TermuxSession nextSession = selectNextVisibleSession();
        if (nextSession != null)
            setCurrentSession(nextSession.getTerminalSession());
    }

    static boolean shouldSwitchSessionAfterRebuild(boolean currentSessionStillPresent) {
        return !currentSessionStillPresent;
    }

    private void recordPersistedSession(TerminalSession terminalSession, PersistedSession persistedSession) {
        mPersistedSessionBySession.put(terminalSession, persistedSession);
        clearReconnectSuppressionForRecreatedSession(terminalSession);
        savePersistedSessions();
    }

    private void clearReconnectSuppressionForRecreatedSession(@Nullable TerminalSession terminalSession) {
        if (terminalSession == null) return;
        TermuxAppSharedPreferences preferences = mActivity.getPreferences();
        if (preferences == null) return;
        preferences.clearSessionUserRemovedTime(terminalSession.mSessionName);
        if (preferences.isSessionUserRemoved(terminalSession.mSessionName)) {
            preferences.setSessionUserRemoved(terminalSession.mSessionName, false);
        }
    }

    private void savePersistedSessions() {
        try {
            List<PersistedSessionRestoreData> restoreData = new ArrayList<>();
            for (Map.Entry<TerminalSession, PersistedSession> entry : mPersistedSessionBySession.entrySet()) {
                TerminalSession session = entry.getKey();
                PersistedSession persistedSession = entry.getValue();
                restoreData.add(new PersistedSessionRestoreData(persistedSession.getHandle(), session.mSessionName,
                    persistedSession.getExecutablePath(), persistedSession.getArguments(),
                    persistedSession.isFailSafe(), persistedSession.getWorkingDirectory()));
            }
            String serialized = mPersistedSessionSerializer.serialize(restoreData);
            mActivity.getPreferences().setPersistedSessions(serialized);
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize persisted sessions", e);
        }
    }

    private List<PersistedSessionRestoreData> loadPersistedSessions() {
        try {
            return mPersistedSessionSerializer.deserialize(mActivity.getPreferences().getPersistedSessions());
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to deserialize persisted sessions, clearing the store", e);
            mActivity.getPreferences().setPersistedSessions(null);
            return new ArrayList<>();
        }
    }

    /**
     * Recreate the sessions persisted in shared preferences. Returns {@code true} if at least one
     * session was restored, in which case the caller should not create the default session.
     */
    public boolean restorePersistedSessions() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;

        List<PersistedSessionRestoreData> persistedSessions = loadPersistedSessions();
        if (persistedSessions.isEmpty()) return false;

        Set<String> restoredNames = new HashSet<>();
        TerminalSession firstRestoredSession = null;

        int configuredLimit = maxSessions();
        int droppedSessionCount = 0;

        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController != null) browserController.beginPersistenceBatch();
        try {
            for (PersistedSessionRestoreData persistedSession : persistedSessions) {
                String name = persistedSession.getName();
                if (name != null && !restoredNames.add(name)) continue;
                if (cappedSessionCount(service) >= configuredLimit) {
                    droppedSessionCount++;
                    continue;
                }

                TermuxSession newTermuxSession = service.createTermuxSession(persistedSession.getExecutablePath(),
                    persistedSession.getArguments(), null, persistedSession.getWorkingDirectory(),
                    persistedSession.isFailSafe(), name);
                if (newTermuxSession == null) continue;

                TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
                mPersistedSessionBySession.put(newTerminalSession, new PersistedSession(newTerminalSession.mHandle,
                    persistedSession.getExecutablePath(), persistedSession.getArguments(),
                    persistedSession.isFailSafe(), persistedSession.getWorkingDirectory()));
                attachBrowserTabForUrlSessionName(newTerminalSession, name);
                if (firstRestoredSession == null)
                    firstRestoredSession = newTerminalSession;
            }
        } finally {
            if (browserController != null) browserController.endPersistenceBatch();
        }

        notifySessionLimitExceeded(configuredLimit, droppedSessionCount);

        if (firstRestoredSession == null) return false;

        savePersistedSessions();
        service.pruneSessionNewActivityStoreToLiveSessions();
        if (shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession()))
            setStartupDisplayedSession(firstRestoredSession);
        mActivity.getDrawer().closeDrawers();
        return true;
    }

    /**
     * Recreate the sessions whose names the user configured as always present. Returns {@code true}
     * if at least one session was created, in which case the caller should not create the default
     * session. This runs on a cold start after the persisted session list has been cleared and again
     * after a session reload rebuilds the session list, so the always-present sessions return without
     * reintroducing the previously-cleared persisted records.
     */
    public boolean restoreAlwaysPresentSessions() {
        return restoreAlwaysPresentSessions(Collections.emptyList());
    }

    public boolean restoreAlwaysPresentSessions(@NonNull Collection<String> additionalAlwaysPresentSessionNames) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;

        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }

        List<String> alwaysPresentSessionNames = new ArrayList<>(additionalAlwaysPresentSessionNames);
        alwaysPresentSessionNames.addAll(mActivity.getPreferences().getAlwaysNaSessionNames());

        List<String> missingSessionNames = mAlwaysPresentSessionPlanner.planMissingSessionNames(
            alwaysPresentSessionNames, liveSessionNames,
            mActivity.getPreferences().getDisabledSessionNames(),
            mActivity.getPreferences().getUserRemovedSessionNames());
        if (missingSessionNames.isEmpty()) return false;

        String commandTemplate = mActivity.getPreferences().getAutosshCommand();
        String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
        String workingDirectory = mActivity.getProperties().getDefaultWorkingDirectory();

        int configuredLimit = maxSessions();
        int droppedSessionCount = 0;

        TerminalSession firstCreatedSession = null;
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController != null) browserController.beginPersistenceBatch();
        try {
            for (String sessionName : missingSessionNames) {
                if (cappedSessionCount(service) >= configuredLimit) {
                    droppedSessionCount++;
                    continue;
                }

                AlwaysPresentSessionStartup startup =
                    mAlwaysPresentSessionStartupPlanner.planStartup(sessionName, commandTemplate, shellPath);
                TermuxSession newTermuxSession = service.createTermuxSession(startup.getExecutablePath(),
                    startup.getArguments(), null, workingDirectory, false, sessionName);
                if (newTermuxSession == null) continue;

                TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
                recordPersistedSession(newTerminalSession, new PersistedSession(newTerminalSession.mHandle,
                    startup.getExecutablePath(), startup.getArguments(), false, workingDirectory));
                attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
                if (firstCreatedSession == null)
                    firstCreatedSession = newTerminalSession;
            }
        } finally {
            if (browserController != null) browserController.endPersistenceBatch();
        }

        notifySessionLimitExceeded(configuredLimit, droppedSessionCount);

        if (firstCreatedSession == null) return false;

        if (shouldSwitchSessionOnReconnect(hasValidCurrentDisplayedSession()))
            setStartupDisplayedSession(firstCreatedSession);
        mActivity.getDrawer().closeDrawers();
        return true;
    }

    /**
     * Rebuild the in-memory persisted session map from the live sessions when the activity reconnects
     * to a service that already has running sessions, matching persisted records to live sessions by
     * their stable handle. This keeps later create/rename/remove updates in sync and drops persisted
     * records whose sessions are no longer running.
     */
    public void syncPersistedSessionsWithLiveSessions() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        Map<String, PersistedSession> persistedSessionByHandle = new HashMap<>();
        for (PersistedSessionRestoreData restoreData : loadPersistedSessions()) {
            if (restoreData.getHandle() != null)
                persistedSessionByHandle.put(restoreData.getHandle(), new PersistedSession(restoreData.getHandle(),
                    restoreData.getExecutablePath(), restoreData.getArguments(), restoreData.isFailSafe(),
                    restoreData.getWorkingDirectory()));
        }

        mPersistedSessionBySession.clear();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            PersistedSession persistedSession = persistedSessionByHandle.get(terminalSession.mHandle);
            if (persistedSession != null)
                mPersistedSessionBySession.put(terminalSession, persistedSession);
        }

        savePersistedSessions();
        service.pruneSessionNewActivityStoreToLiveSessions();

        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController != null) browserController.beginPersistenceBatch();
        try {
            for (TermuxSession termuxSession : service.getTermuxSessions()) {
                TerminalSession terminalSession = termuxSession.getTerminalSession();
                attachBrowserTabForUrlSessionName(terminalSession, terminalSession.mSessionName);
            }
        } finally {
            if (browserController != null) browserController.endPersistenceBatch();
        }
    }

    public void termuxSessionListNotifyUpdated() {
        mActivity.termuxSessionListNotifyUpdated();
    }


    String toToastTitle(TerminalSession session) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return null;

        final int indexOfSession = service.getIndexOfSession(session);
        if (indexOfSession < 0) return null;
        StringBuilder toastTitle = new StringBuilder("[" + (indexOfSession + 1) + "]");
        if (!TextUtils.isEmpty(session.mSessionName)) {
            toastTitle.append(" ").append(session.mSessionName);
        }
        String title = session.getTitle();
        if (!TextUtils.isEmpty(title)) {
            // Space to "[${NR}] or newline after session name:
            toastTitle.append(session.mSessionName == null ? " " : "\n");
            toastTitle.append(title);
        }
        return toastTitle.toString();
    }


    public void checkForFontAndColors() {
        try {
            File colorsFile = TermuxConstants.TERMUX_COLOR_PROPERTIES_FILE;
            File fontFile = TermuxConstants.TERMUX_FONT_FILE;

            final Properties props = new Properties();
            if (colorsFile.isFile()) {
                try (InputStream in = new FileInputStream(colorsFile)) {
                    props.load(in);
                }
            }

            TerminalColors.COLOR_SCHEME.updateWith(props);
            TerminalSession session = mActivity.getCurrentSession();
            if (session != null && session.getEmulator() != null) {
                session.getEmulator().mColors.reset();
            }
            updateBackgroundColor();

            final Typeface newTypeface = (fontFile.exists() && fontFile.length() > 0) ? Typeface.createFromFile(fontFile) : Typeface.MONOSPACE;
            mActivity.getTerminalView().setTypeface(newTypeface);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Error in checkForFontAndColors()", e);
        }
    }

    public void updateBackgroundColor() {
        if (!mActivity.isVisible()) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null && session.getEmulator() != null) {
            mActivity.getWindow().getDecorView().setBackgroundColor(session.getEmulator().mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]);
        }
    }

}
