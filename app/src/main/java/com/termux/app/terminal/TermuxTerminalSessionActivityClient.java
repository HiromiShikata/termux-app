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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.OpenTagBrowserController;
import com.termux.app.browser.ProjectBrowserOverlayController;
import com.termux.app.browser.ProjectBrowserSessionDismissal;
import com.termux.app.browser.SessionNameBrowserTabUrlResolver;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.app.sessiondefinition.SessionDefinitionPlannedSession;
import com.termux.app.sessiondefinition.SessionDefinitionPlanner;
import com.termux.app.terminal.io.TerminalToolbarViewPager;
import com.termux.app.terminal.session.AlwaysPresentSessionPlanner;
import com.termux.app.terminal.session.AlwaysPresentSessionStartup;
import com.termux.app.terminal.session.AlwaysPresentSessionStartupPlanner;
import com.termux.app.terminal.session.DuplicateSessionNameResolution;
import com.termux.app.terminal.session.DuplicateSessionNameResolver;
import com.termux.app.terminal.session.PersistedSession;
import com.termux.app.terminal.tts.TtsManager;
import com.termux.app.terminal.session.PersistedSessionSerializer;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.shared.termux.TermuxConstants;
import com.termux.app.TermuxService;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.terminal.io.BellHandler;
import com.termux.shared.logger.Logger;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.terminal.TextStyle;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** The {@link TerminalSessionClient} implementation that may require an {@link Activity} for its interface methods. */
public class TermuxTerminalSessionActivityClient extends TermuxTerminalSessionClientBase {

    private final TermuxActivity mActivity;

    private final PersistedSessionSerializer mPersistedSessionSerializer = new PersistedSessionSerializer();

    private final SessionDefinitionEntryMatcher mSessionDefinitionEntryMatcher = new SessionDefinitionEntryMatcher();

    private final SessionNameBrowserTabUrlResolver mSessionNameBrowserTabUrlResolver = new SessionNameBrowserTabUrlResolver();

    private final AlwaysPresentSessionPlanner mAlwaysPresentSessionPlanner = new AlwaysPresentSessionPlanner();
    private final AlwaysPresentSessionStartupPlanner mAlwaysPresentSessionStartupPlanner = new AlwaysPresentSessionStartupPlanner();

    private final LinkedHashMap<TerminalSession, PersistedSession> mPersistedSessionBySession = new LinkedHashMap<>();

    private static final int MAX_SESSIONS = 32;

    private SoundPool mBellSoundPool;

    private int mBellSoundId;

    private static final String LOG_TAG = "TermuxTerminalSessionActivityClient";

    private static final float SESSION_NAME_BAR_TITLE_RELATIVE_SIZE = 0.7f;

    private static final long OUTPUT_ACTIVITY_REFRESH_INTERVAL_MILLIS = 500L;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final SessionOutputActivityRefreshDebouncer mOutputActivityRefreshDebouncer =
        new SessionOutputActivityRefreshDebouncer(OUTPUT_ACTIVITY_REFRESH_INTERVAL_MILLIS);

    private String mActiveSessionHandle;

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
        // otherwise get the last session currently running.
        if (mActivity.getTermuxService() != null) {
            setCurrentSession(getCurrentStoredSessionOrLast());
            termuxSessionListNotifyUpdated();
        }

        // The current terminal session may have changed while being away, force
        // a refresh of the displayed terminal.
        mActivity.getTerminalView().onScreenUpdated();
    }

    /**
     * Should be called when mActivity.onResume() is called
     */
    public void onResume() {
        // Just initialize the mBellSoundPool and load the sound, otherwise bell might not run
        // the first time bell key is pressed and play() is called, since sound may not be loaded
        // quickly enough before the call to play(). https://stackoverflow.com/questions/35435625
        loadBellSoundPool();
    }

    /**
     * Should be called when mActivity.onStop() is called
     */
    public void onStop() {
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
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        boolean isCurrentSession = mActivity.getCurrentSession() == changedSession;

        if (!isCurrentSession)
            recordOutputActivityForBackgroundSession(changedSession);

        if (!mActivity.isVisible()) return;

        if (isCurrentSession) {
            mActivity.getTerminalView().onScreenUpdated();
            openTagsForSession(changedSession);
        }
    }

    private void recordOutputActivityForBackgroundSession(@NonNull TerminalSession session) {
        if (!markNewActivityForBackgroundSession(session)) return;
        if (mActivity.isVisible()
            && mOutputActivityRefreshDebouncer.shouldRefresh(System.currentTimeMillis()))
            termuxSessionListNotifyUpdated();
    }

    private void openTagsForSession(TerminalSession session) {
        if (session == null) return;

        OpenTagBrowserController openTagBrowserController = mActivity.getOpenTagBrowserController();
        if (openTagBrowserController == null) return;
        if (!openTagBrowserController.isAutoOpenEnabled()) return;

        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;

        TerminalBuffer screen = emulator.getScreen();
        if (screen == null) return;

        openTagBrowserController.onSessionTextChanged(session.mHandle, screen.getTranscriptText());
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

        if (mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // On Android TV devices we need to use older behaviour because we may
            // not be able to have multiple launcher icons.
            if (service.getTermuxSessionsSize() > 1 || isPluginExecutionCommandWithPendingResult) {
                removeFinishedSession(finishedSession);
            }
        } else {
            // Once we have a separate launcher icon for the failsafe session, it
            // should be safe to auto-close session on exit code '0' or '130'.
            if (finishedSession.getExitStatus() == 0 || finishedSession.getExitStatus() == 130 || isPluginExecutionCommandWithPendingResult) {
                removeFinishedSession(finishedSession);
            }
        }
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
        recordBellNotificationIfBackgroundSession(session);

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
    public void onMarkerNotification(@NonNull TerminalSession session) {
        recordBellNotificationIfBackgroundSession(session);
    }

    @Override
    public void onUrgentNotification(@NonNull TerminalSession session) {
        recordBellNotificationIfBackgroundSession(session);
        mMainThreadHandler.post(() -> handleUrgentNotificationOnMainThread(session));
    }

    @Override
    public void onSpeakNotification(@NonNull TerminalSession session, @NonNull String text) {
        if (mActivity.getCurrentSession() != session) return;
        if (!mActivity.getPreferences().isSpeakTagAutoReadEnabled()) return;
        TtsManager ttsManager = mActivity.getTtsManager();
        if (ttsManager == null) return;
        ttsManager.speak(text);
    }

    private void handleUrgentNotificationOnMainThread(@NonNull TerminalSession session) {
        playUrgentNotificationSound();
        if (session.mHandle != null)
            mActivity.getPreferences().setCurrentSession(session.mHandle);
        TermuxActivity.startTermuxActivity(mActivity);
        forceDisplaySession(session);
        mMainThreadHandler.post(() -> forceDisplaySession(session));
    }

    private void forceDisplaySession(@NonNull TerminalSession session) {
        setCurrentSession(session);
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController != null)
            browserController.showTerminal();
    }

    private void playUrgentNotificationSound() {
        Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (notificationUri == null) return;

        Ringtone ringtone = RingtoneManager.getRingtone(mActivity, notificationUri);
        if (ringtone == null) return;
        ringtone.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        ringtone.play();
    }

    private void recordBellNotificationIfBackgroundSession(@NonNull TerminalSession session) {
        if (markNewActivityForBackgroundSession(session))
            termuxSessionListNotifyUpdated();
    }

    private boolean markNewActivityForBackgroundSession(@NonNull TerminalSession session) {
        if (session.mHandle == null) return false;
        if (session.mHandle.equals(activeSessionHandle())) return false;

        mActivity.getSessionNewActivityStore().markNewActivity(session.mHandle, System.currentTimeMillis());
        return true;
    }

    @Nullable
    private String activeSessionHandle() {
        if (mActiveSessionHandle != null) {
            return mActiveSessionHandle;
        }
        TerminalSession currentSession = mActivity.getCurrentSession();
        return currentSession == null ? null : currentSession.mHandle;
    }

    private void clearNewActivityForViewedSession(@Nullable String sessionHandle) {
        if (sessionHandle == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (!store.hasNewActivity(sessionHandle)) return;
        store.clearNewActivity(sessionHandle);
        termuxSessionListNotifyUpdated();
    }

    private void purgeNewActivityForRemovedSession(@Nullable String sessionHandle) {
        if (sessionHandle == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (!store.hasNewActivity(sessionHandle)) return;
        store.purgeSession(sessionHandle);
        termuxSessionListNotifyUpdated();
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

        mActiveSessionHandle = session.mHandle;

        clearNewActivityForViewedSession(session.mHandle);

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

        if (toolbarAdapter != null && switchingSessions)
            toolbarAdapter.restoreTextInputForSession(session);

        termuxSessionListNotifyUpdated();
        updateBackgroundColor();

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionChanged(session);

        SessionListBottomSheetController sessionListBottomSheetController = mActivity.getSessionListBottomSheetController();
        if (sessionListBottomSheetController != null)
            sessionListBottomSheetController.revealCurrentSessionRowIfShowing();

        ProjectBrowserOverlayController projectBrowser = mActivity.getProjectBrowserOverlayController();
        if (projectBrowser != null
            && ProjectBrowserSessionDismissal.shouldDismissOnSessionAccess(projectBrowser.isVisible()))
            projectBrowser.hide();

        openTagsForSession(session);

        enforceActiveSessionViewBinding(session);

        updateSessionNameOverlay();
    }

    private void enforceActiveSessionViewBinding(@NonNull TerminalSession session) {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();

        TerminalSession displayedTerminalSession =
            (mActivity.getTerminalView() == null) ? null : mActivity.getTerminalView().getCurrentSession();
        String displayedTerminalSessionHandle =
            (displayedTerminalSession == null) ? null : displayedTerminalSession.mHandle;
        boolean browserVisible = browserController != null && browserController.isBrowserVisible();
        String displayedBrowserSessionHandle =
            (browserController == null) ? null : browserController.getDisplayedSessionHandle();
        boolean activeSessionHasBrowserTab =
            browserController != null && browserController.hasBrowserTabForSession(mActiveSessionHandle);

        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            mActiveSessionHandle, displayedTerminalSessionHandle, browserVisible,
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
        if (!SessionNameBarVisibility.isVisible(sessionName)) {
            sessionNameBar.setText("");
            sessionNameBar.setVisibility(View.GONE);
            sessionNameBar.setOnClickListener(null);
            sessionNameBar.setClickable(false);
            updateSessionProjectStoryBar(null);
        } else {
            String title = mSessionDefinitionEntryMatcher.findTitleForSessionName(
                mActivity.getSessionDefinitionEntries(), sessionName);
            SessionNameBarContent content = SessionNameBarContent.of(sessionName, title);
            sessionNameBar.setSingleLine(false);
            sessionNameBar.setMaxLines(content.hasTitle() ? 3 : 2);
            sessionNameBar.setText(buildSessionNameBarText(content));
            sessionNameBar.setVisibility(View.VISIBLE);
            sessionNameBar.setOnClickListener(view -> onSessionNameBarTapped());
            updateSessionProjectStoryBar(sessionName);
        }
    }

    private void updateSessionProjectStoryBar(@Nullable String sessionName) {
        TextView projectStoryBar = mActivity.findViewById(R.id.session_project_story_bar);
        if (projectStoryBar == null) return;

        SessionDefinitionEntry entry = mSessionDefinitionEntryMatcher.findEntryForSessionName(
            mActivity.getSessionDefinitionEntries(), sessionName);
        SessionProjectStoryLine line = (entry == null)
            ? SessionProjectStoryLine.of(null, null)
            : SessionProjectStoryLine.of(entry.getGroupLabel(), entry.getEntryLabel());
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
        spannable.setSpan(new RelativeSizeSpan(SESSION_NAME_BAR_TITLE_RELATIVE_SIZE),
            content.getTitleStart(), content.getTitleEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private void onSessionNameBarTapped() {
        ProjectBrowserOverlayController projectBrowser = mActivity.getProjectBrowserOverlayController();
        boolean projectBrowserVisible = projectBrowser != null && projectBrowser.isVisible();
        String projectBrowserUrl = (projectBrowser == null) ? null : projectBrowser.getCurrentUrl();
        TerminalSession session = mActivity.getCurrentSession();
        String sessionNameForCopy = (session == null) ? null
            : resolveSessionNameForCopy(session.mSessionName, session.getTitle());

        HeaderTapCopyTarget target = HeaderTapCopyTarget.resolve(
            projectBrowserVisible, projectBrowserUrl, sessionNameForCopy);
        if (target.isEmpty()) return;

        int confirmationMessageResId = target.isProjectBrowserUrl()
            ? R.string.msg_browser_url_copied
            : R.string.msg_session_name_copied_to_clipboard;
        ShareUtils.copyTextToClipboard(mActivity, target.getText(),
            mActivity.getString(confirmationMessageResId));
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

        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.title_confirm_delete_session)
            .setMessage(R.string.msg_confirm_delete_session)
            .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                dialog.dismiss();
                TerminalSession currentSession = mActivity.getCurrentSession();
                sessionToDelete.finishIfRunning();
                removeFinishedSession(sessionToDelete);
                if (currentSession != null && currentSession != sessionToDelete)
                    setCurrentSession(currentSession);
            })
            .setNegativeButton(android.R.string.no, null));
    }

    private void renameSession(TerminalSession sessionToRename, String text) {
        if (sessionToRename == null) return;
        sessionToRename.mSessionName = text;
        TermuxService service = mActivity.getTermuxService();
        if (service != null) {
            TermuxSession termuxSession = service.getTermuxSessionForTerminalSession(sessionToRename);
            if (termuxSession != null)
                termuxSession.getExecutionCommand().shellName = text;
        }

        if (sessionToRename == mActivity.getCurrentSession())
            updateSessionNameOverlay();

        updatePersistedSessionName(sessionToRename, text);
    }

    public void addNewSession(boolean isFailSafe, String sessionName) {
        addNewSession(isFailSafe, sessionName, true);
    }

    public void addNewSession(boolean isFailSafe, String sessionName, boolean closeDrawerAfter) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        if (revealExistingSessionByName(sessionName, closeDrawerAfter)) return;

        if (service.getTermuxSessionsSize() >= MAX_SESSIONS) {
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity).setTitle(R.string.title_max_terminals_reached).setMessage(R.string.msg_max_terminals_reached)
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
                recordPersistedSession(newTerminalSession, new PersistedSession(newTerminalSession.mHandle, sessionName, null, null, false, workingDirectory));
            attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
            setCurrentSession(newTerminalSession);

            if (closeDrawerAfter)
                mActivity.getDrawer().closeDrawers();
        }
    }

    public void addNewSessionApplyingAutosshConfig(String sessionName) {
        addNewSessionApplyingAutosshConfig(sessionName, true);
    }

    public void addNewSessionApplyingAutosshConfig(String sessionName, boolean closeDrawerAfter) {
        String commandTemplate = mActivity.getPreferences().getAutosshCommand();
        SessionDefinitionPlannedSession plannedSession =
            new SessionDefinitionPlanner().planNamedSession(sessionName, commandTemplate);
        if (plannedSession.hasCommand()) {
            addNewAutosshSession(plannedSession.getName(), plannedSession.getCommand(), closeDrawerAfter);
        } else {
            addNewSession(false, plannedSession.getName(), closeDrawerAfter);
        }
    }

    public void addNewAutosshSession(String sessionName, String command) {
        addNewAutosshSession(sessionName, command, true);
    }

    public void addNewAutosshSession(String sessionName, String command, boolean closeDrawerAfter) {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        if (revealExistingSessionByName(sessionName, closeDrawerAfter)) return;

        if (service.getTermuxSessionsSize() >= MAX_SESSIONS) {
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity).setTitle(R.string.title_max_terminals_reached).setMessage(R.string.msg_max_terminals_reached)
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
            recordPersistedSession(newTerminalSession, new PersistedSession(newTerminalSession.mHandle, sessionName, shellPath, arguments, false, workingDirectory));
            attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
            setCurrentSession(newTerminalSession);

            if (closeDrawerAfter)
                mActivity.getDrawer().closeDrawers();
        }
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

    private void attachBrowserTabForUrlSessionName(@NonNull TerminalSession session, @Nullable String sessionName) {
        String browserTabUrl = mSessionNameBrowserTabUrlResolver.resolve(sessionName);
        if (browserTabUrl == null) return;
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) return;
        browserController.attachBackgroundTab(session.mHandle, browserTabUrl);
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
            purgeNewActivityForRemovedSession(finishedSession.mHandle);

        if (mActivity.getTermuxBrowserController() != null)
            mActivity.getTermuxBrowserController().onSessionRemoved(finishedSession);

        if (mPersistedSessionBySession.remove(finishedSession) != null)
            savePersistedSessions();

        service.removeTermuxSession(finishedSession);

        int size = service.getTermuxSessionsSize();
        if (size == 0) {
            // There are no sessions to show, so finish the activity.
            mActivity.finishActivityIfNotFinishing();
        } else {
            TermuxSession nextSession = selectNextVisibleSession();
            if (nextSession != null)
                setCurrentSession(nextSession.getTerminalSession());
        }

        TerminalToolbarViewPager.PageAdapter toolbarAdapter = mActivity.getTerminalToolbarViewPagerAdapter();
        if (toolbarAdapter != null)
            toolbarAdapter.removeTextInputForSession(finishedSession);
    }

    public void removeSessionForRebuild(TerminalSession sessionToRemove) {
        if (sessionToRemove == null) return;
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        purgeNewActivityForRemovedSession(sessionToRemove.mHandle);

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

    public void ensureCurrentSessionValidAfterRebuild() {
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        TerminalSession currentSession = mActivity.getCurrentSession();
        if (currentSession != null && service.getIndexOfSession(currentSession) >= 0) return;

        TermuxSession nextSession = selectNextVisibleSession();
        if (nextSession != null)
            setCurrentSession(nextSession.getTerminalSession());
    }

    private void recordPersistedSession(TerminalSession terminalSession, PersistedSession persistedSession) {
        mPersistedSessionBySession.put(terminalSession, persistedSession);
        savePersistedSessions();
    }

    private void updatePersistedSessionName(TerminalSession terminalSession, String name) {
        PersistedSession existing = mPersistedSessionBySession.get(terminalSession);
        if (existing == null) return;

        mPersistedSessionBySession.put(terminalSession, new PersistedSession(existing.getHandle(), name,
            existing.getExecutablePath(), existing.getArguments(), existing.isFailSafe(), existing.getWorkingDirectory()));
        savePersistedSessions();
    }

    private void savePersistedSessions() {
        try {
            String serialized = mPersistedSessionSerializer.serialize(new ArrayList<>(mPersistedSessionBySession.values()));
            mActivity.getPreferences().setPersistedSessions(serialized);
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to serialize persisted sessions", e);
        }
    }

    private List<PersistedSession> loadPersistedSessions() {
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

        List<PersistedSession> persistedSessions = loadPersistedSessions();
        if (persistedSessions.isEmpty()) return false;

        Set<String> restoredNames = new HashSet<>();
        TerminalSession firstRestoredSession = null;

        for (PersistedSession persistedSession : persistedSessions) {
            String name = persistedSession.getName();
            if (name != null && !restoredNames.add(name)) continue;
            if (service.getTermuxSessionsSize() >= MAX_SESSIONS) break;

            TermuxSession newTermuxSession = service.createTermuxSession(persistedSession.getExecutablePath(),
                persistedSession.getArguments(), null, persistedSession.getWorkingDirectory(),
                persistedSession.isFailSafe(), name);
            if (newTermuxSession == null) continue;

            TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
            mPersistedSessionBySession.put(newTerminalSession, new PersistedSession(newTerminalSession.mHandle, name,
                persistedSession.getExecutablePath(), persistedSession.getArguments(), persistedSession.isFailSafe(),
                persistedSession.getWorkingDirectory()));
            attachBrowserTabForUrlSessionName(newTerminalSession, name);
            if (firstRestoredSession == null)
                firstRestoredSession = newTerminalSession;
        }

        if (firstRestoredSession == null) return false;

        savePersistedSessions();
        setCurrentSession(firstRestoredSession);
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
        TermuxService service = mActivity.getTermuxService();
        if (service == null) return false;

        List<String> liveSessionNames = new ArrayList<>();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            liveSessionNames.add(termuxSession.getTerminalSession().mSessionName);
        }

        List<String> missingSessionNames = mAlwaysPresentSessionPlanner.planMissingSessionNames(
            mActivity.getPreferences().getAlwaysNaSessionNames(), liveSessionNames);
        if (missingSessionNames.isEmpty()) return false;

        String commandTemplate = mActivity.getPreferences().getAutosshCommand();
        String shellPath = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
        String workingDirectory = mActivity.getProperties().getDefaultWorkingDirectory();

        TerminalSession firstCreatedSession = null;
        for (String sessionName : missingSessionNames) {
            if (service.getTermuxSessionsSize() >= MAX_SESSIONS) break;

            AlwaysPresentSessionStartup startup =
                mAlwaysPresentSessionStartupPlanner.planStartup(sessionName, commandTemplate, shellPath);
            TermuxSession newTermuxSession = service.createTermuxSession(startup.getExecutablePath(),
                startup.getArguments(), null, workingDirectory, false, sessionName);
            if (newTermuxSession == null) continue;

            TerminalSession newTerminalSession = newTermuxSession.getTerminalSession();
            recordPersistedSession(newTerminalSession, new PersistedSession(newTerminalSession.mHandle,
                sessionName, startup.getExecutablePath(), startup.getArguments(), false, workingDirectory));
            attachBrowserTabForUrlSessionName(newTerminalSession, sessionName);
            if (firstCreatedSession == null)
                firstCreatedSession = newTerminalSession;
        }

        if (firstCreatedSession == null) return false;

        setCurrentSession(firstCreatedSession);
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
        for (PersistedSession persistedSession : loadPersistedSessions()) {
            if (persistedSession.getHandle() != null)
                persistedSessionByHandle.put(persistedSession.getHandle(), persistedSession);
        }

        mPersistedSessionBySession.clear();
        for (TermuxSession termuxSession : service.getTermuxSessions()) {
            TerminalSession terminalSession = termuxSession.getTerminalSession();
            PersistedSession persistedSession = persistedSessionByHandle.get(terminalSession.mHandle);
            if (persistedSession != null)
                mPersistedSessionBySession.put(terminalSession, persistedSession);
        }

        savePersistedSessions();
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
