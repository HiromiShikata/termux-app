package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.browser.TermuxBrowserController;
import com.termux.shared.interact.DialogUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase;
import com.termux.shared.termux.extrakeys.SpecialButton;
import com.termux.app.terminal.io.KeyboardShortcut;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.data.DataUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.data.TermuxUrlUtils;
import com.termux.shared.view.KeyboardUtils;
import com.termux.shared.view.ViewUtils;
import com.termux.terminal.KeyHandler;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import androidx.drawerlayout.widget.DrawerLayout;

public class TermuxTerminalViewClient extends TermuxTerminalViewClientBase {

    final TermuxActivity mActivity;

    final TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    /** Keeping track of the special keys acting as Ctrl and Fn for the soft keyboard and other hardware keys. */
    boolean mVirtualControlKeyDown, mVirtualFnKeyDown;

    private Runnable mShowSoftKeyboardRunnable;

    private boolean mShowSoftKeyboardIgnoreOnce;
    private boolean mShowSoftKeyboardWithDelayOnce;

    private boolean mTerminalCursorBlinkerStateAlreadySet;

    private List<KeyboardShortcut> mSessionShortcuts;

    private String mLongPressedUrl;

    private static final String LOG_TAG = "TermuxTerminalViewClient";

    public TermuxTerminalViewClient(TermuxActivity activity, TermuxTerminalSessionActivityClient termuxTerminalSessionActivityClient) {
        this.mActivity = activity;
        this.mTermuxTerminalSessionActivityClient = termuxTerminalSessionActivityClient;
    }

    public TermuxActivity getActivity() {
        return mActivity;
    }

    /**
     * Should be called when mActivity.onCreate() is called
     */
    public void onCreate() {
        onReloadProperties();

        mActivity.getTerminalView().setTextSize(mActivity.getPreferences().getFontSize());
        mActivity.getTerminalView().setKeepScreenOn(mActivity.getPreferences().shouldKeepScreenOn());
    }

    /**
     * Should be called when mActivity.onStart() is called
     */
    public void onStart() {
        mActivity.getTerminalView().setKeepScreenOn(mActivity.getPreferences().shouldKeepScreenOn());

        // Set {@link TerminalView#TERMINAL_VIEW_KEY_LOGGING_ENABLED} value
        // Also required if user changed the preference from {@link TermuxSettings} activity and returns
        boolean isTerminalViewKeyLoggingEnabled = mActivity.getPreferences().isTerminalViewKeyLoggingEnabled();
        mActivity.getTerminalView().setIsTerminalViewKeyLoggingEnabled(isTerminalViewKeyLoggingEnabled);

        // Piggyback on the terminal view key logging toggle for now, should add a separate toggle in future
        mActivity.getTermuxActivityRootView().setIsRootViewLoggingEnabled(isTerminalViewKeyLoggingEnabled);
        ViewUtils.setIsViewUtilsLoggingEnabled(isTerminalViewKeyLoggingEnabled);
    }

    /**
     * Should be called when mActivity.onResume() is called
     */
    public void onResume() {
        // Show the soft keyboard if required
        setSoftKeyboardState(true, mActivity.isActivityRecreated());

        mTerminalCursorBlinkerStateAlreadySet = false;

        if (mActivity.getTerminalView().mEmulator != null) {
            // Start terminal cursor blinking if enabled
            // If emulator is already set, then start blinker now, otherwise wait for onEmulatorSet()
            // event to start it. This is needed since onEmulatorSet() may not be called after
            // TermuxActivity is started after device display timeout with double tap and not power button.
            setTerminalCursorBlinkerState(true);
            mTerminalCursorBlinkerStateAlreadySet = true;
        }
    }

    /**
     * Should be called when mActivity.onStop() is called
     */
    public void onStop() {
        // Stop terminal cursor blinking if enabled
        setTerminalCursorBlinkerState(false);
    }

    /**
     * Should be called when mActivity.reloadProperties() is called
     */
    public void onReloadProperties() {
        setSessionShortcuts();
    }

    /**
     * Should be called when mActivity.reloadActivityStyling() is called
     */
    public void onReloadActivityStyling() {
        // Show the soft keyboard if required
        setSoftKeyboardState(false, true);

        // Start terminal cursor blinking if enabled
        setTerminalCursorBlinkerState(true);
    }

    /**
     * Should be called when {@link com.termux.view.TerminalView#mEmulator} is set
     */
    @Override
    public void onEmulatorSet() {
        if (!mTerminalCursorBlinkerStateAlreadySet) {
            // Start terminal cursor blinking if enabled
            // We need to wait for the first session to be attached that's set in
            // TermuxActivity.onServiceConnected() and then the multiple calls to TerminalView.updateSize()
            // where the final one eventually sets the mEmulator when width/height is not 0. Otherwise
            // blinker will not start again if TermuxActivity is started again after exiting it with
            // double back press. Check TerminalView.setTerminalCursorBlinkerState().
            setTerminalCursorBlinkerState(true);
            mTerminalCursorBlinkerStateAlreadySet = true;
        }
    }



    @Override
    public float onScale(float scale) {
        switch (ScaleGestureFontSizeDecision.decide(scale)) {
            case INCREASE_FONT_SIZE:
                changeFontSize(true);
                return 1.0f;
            case DECREASE_FONT_SIZE:
                changeFontSize(false);
                return 1.0f;
            case NO_CHANGE:
            default:
                return scale;
        }
    }



    @Override
    public void onSingleTapUp(MotionEvent e) {
        TerminalEmulator term = mActivity.getCurrentSession().getEmulator();

        if (mActivity.getProperties().shouldOpenTerminalTranscriptURLOnClick()) {
            int[] columnAndRow = mActivity.getTerminalView().getColumnAndRow(e, true);

            String hyperlinkUri = term.getScreen().getHyperlinkUri(columnAndRow[1], columnAndRow[0]);
            if (hyperlinkUri != null) {
                openUrlInApp(hyperlinkUri);
                return;
            }

            String wordAtTap = term.getScreen().getWordAtLocation(columnAndRow[0], columnAndRow[1]);
            LinkedHashSet<CharSequence> urlSet = TermuxUrlUtils.extractUrls(wordAtTap);

            if (!urlSet.isEmpty()) {
                String url = (String) urlSet.iterator().next();
                ShareUtils.openUrl(mActivity, url);
                return;
            }
        }

        if (!term.isMouseTrackingActive() && !e.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (!KeyboardUtils.areDisableSoftKeyboardFlagsSet(mActivity))
                KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
            else
                Logger.logVerbose(LOG_TAG, "Not showing soft keyboard onSingleTapUp since its disabled");
        }
    }

    @Override
    public boolean shouldBackButtonBeMappedToEscape() {
        return mActivity.getProperties().isBackKeyTheEscapeKey();
    }

    @Override
    public boolean shouldEnforceCharBasedInput() {
        return mActivity.getProperties().isEnforcingCharBasedInput();
    }

    @Override
    public boolean shouldUseCtrlSpaceWorkaround() {
        return mActivity.getProperties().isUsingCtrlSpaceWorkaround();
    }

    @Override
    public boolean isTerminalViewSelected() {
        return mActivity.getTerminalToolbarViewPager() == null || mActivity.isTerminalViewSelected() || mActivity.getTerminalView().hasFocus();
    }



    @Override
    public void copyModeChanged(boolean copyMode) {
        // Disable drawer while copying.
        mActivity.getDrawer().setDrawerLockMode(copyMode ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
    }



    @SuppressLint("RtlHardcoded")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession currentSession) {
        if (handleVirtualKeys(keyCode, e, true)) return true;

        if (keyCode == KeyEvent.KEYCODE_ENTER && !currentSession.isRunning()) {
            mTermuxTerminalSessionActivityClient.removeFinishedSession(currentSession);
            return true;
        } else if (!mActivity.getProperties().areHardwareKeyboardShortcutsDisabled() &&
            e.isCtrlPressed() && e.isAltPressed()) {
            // Get the unmodified code point:
            int unicodeChar = e.getUnicodeChar(0);
            int shiftedUnicodeChar = e.getUnicodeChar(KeyEvent.META_SHIFT_ON);

            HardwareKeyboardShortcut.Result shortcut =
                HardwareKeyboardShortcut.decide(keyCode, unicodeChar, shiftedUnicodeChar);
            switch (shortcut.action) {
                case SWITCH_TO_NEXT_SESSION:
                    mTermuxTerminalSessionActivityClient.switchToSession(true);
                    break;
                case SWITCH_TO_PREVIOUS_SESSION:
                    mTermuxTerminalSessionActivityClient.switchToSession(false);
                    break;
                case OPEN_DRAWER:
                    mActivity.getDrawer().openDrawer(Gravity.LEFT);
                    break;
                case CLOSE_DRAWER:
                    mActivity.getDrawer().closeDrawers();
                    break;
                case TOGGLE_SOFT_KEYBOARD:
                    onToggleSoftKeyboardRequest();
                    break;
                case SHOW_CONTEXT_MENU:
                    mActivity.getTerminalView().showContextMenu();
                    break;
                case RENAME_SESSION:
                    mTermuxTerminalSessionActivityClient.renameSession(currentSession);
                    break;
                case CREATE_SESSION:
                    mTermuxTerminalSessionActivityClient.addNewSession(false, null);
                    break;
                case SHOW_URL_SELECTION:
                    showUrlSelection();
                    break;
                case PASTE:
                    doPaste();
                    break;
                case INCREASE_FONT_SIZE:
                    changeFontSize(true);
                    break;
                case DECREASE_FONT_SIZE:
                    changeFontSize(false);
                    break;
                case SWITCH_TO_SESSION_INDEX:
                    mTermuxTerminalSessionActivityClient.switchToSession(shortcut.sessionIndex);
                    break;
                case NONE:
                default:
                    break;
            }
            return true;
        }

        return false;

    }



    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        // If emulator is not set, like if bootstrap installation failed and user dismissed the error
        // dialog, then just exit the activity, otherwise they will be stuck in a broken state.
        if (keyCode == KeyEvent.KEYCODE_BACK && mActivity.getTerminalView().mEmulator == null) {
            mActivity.finishActivityIfNotFinishing();
            return true;
        }

        return handleVirtualKeys(keyCode, e, false);
    }

    /**
     * Drive the volume-key session picker, if the user enabled it. The first Volume Up or Volume Down press
     * shows a transient picker overlay highlighting the current session without switching. Each subsequent
     * press moves the highlight to the previous (Volume Up) or next (Volume Down) session within the visible
     * session structure, skipping sessions inside collapsed project groups and wrapping at the ends. When no
     * volume key is pressed for the commit delay, the highlighted session becomes the active session and the
     * overlay hides. The event is consumed on both key down and key up so the system volume does not change
     * and no system volume UI flashes. This is invoked from the activity's dispatchKeyEvent so it works
     * regardless of which view currently has focus, including the terminal toolbar text input.
     */
    public boolean handleVolumeKeysSwitchSessions(int keyCode, boolean down) {
        SessionSwitchPickerController pickerController = mActivity.getSessionSwitchPickerController();
        switch (VolumeKeysSessionSwitchDecision.decide(
                mActivity.getPreferences().isVolumeKeysSwitchSessionsEnabled(), keyCode, down)) {
            case SWITCH_TO_NEXT_SESSION:
                if (pickerController != null) pickerController.onVolumeKeyDirection(true);
                return true;
            case SWITCH_TO_PREVIOUS_SESSION:
                if (pickerController != null) pickerController.onVolumeKeyDirection(false);
                return true;
            case CONSUME_WITHOUT_SWITCH:
                return true;
            case IGNORE:
            default:
                return false;
        }
    }

    /** Handle dedicated volume buttons as virtual keys if applicable. */
    private boolean handleVirtualKeys(int keyCode, KeyEvent event, boolean down) {
        InputDevice inputDevice = event.getDevice();
        if (mActivity.getProperties().areVirtualVolumeKeysDisabled()) {
            return false;
        } else if (inputDevice != null && inputDevice.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
            // Do not steal dedicated buttons from a full external keyboard.
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mVirtualControlKeyDown = down;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mVirtualFnKeyDown = down;
            return true;
        }
        return false;
    }



    @Override
    public boolean readControlKey() {
        return readExtraKeysSpecialButton(SpecialButton.CTRL) || mVirtualControlKeyDown;
    }

    @Override
    public boolean readAltKey() {
        return readExtraKeysSpecialButton(SpecialButton.ALT);
    }

    @Override
    public boolean readShiftKey() {
        return readExtraKeysSpecialButton(SpecialButton.SHIFT);
    }

    @Override
    public boolean readFnKey() {
        return readExtraKeysSpecialButton(SpecialButton.FN);
    }

    public boolean readExtraKeysSpecialButton(SpecialButton specialButton) {
        if (mActivity.getExtraKeysView() == null) return false;
        Boolean state = mActivity.getExtraKeysView().readSpecialButton(specialButton, true);
        if (state == null) {
            Logger.logError(LOG_TAG,"Failed to read an unregistered " + specialButton + " special button value from extra keys.");
            return false;
        }
        return state;
    }

    @Override
    public boolean onLongPress(MotionEvent event) {
        String url = extractUrlAt(event);
        if (DataUtils.isNullOrEmpty(url)) {
            mLongPressedUrl = null;
            return false;
        }
        mLongPressedUrl = url;
        mActivity.getTerminalView().showContextMenu();
        return true;
    }

    private String extractUrlAt(MotionEvent event) {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return null;
        TerminalEmulator term = session.getEmulator();
        if (term == null) return null;

        int[] columnAndRow = mActivity.getTerminalView().getColumnAndRow(event, true);

        String hyperlinkUri = term.getScreen().getHyperlinkUri(columnAndRow[1], columnAndRow[0]);
        String wordAtLongPress = term.getScreen().getWordAtLocation(columnAndRow[0], columnAndRow[1]);
        return selectLongPressUrl(hyperlinkUri, wordAtLongPress);
    }

    static String selectLongPressUrl(String hyperlinkUri, String wordAtLongPress) {
        if (!DataUtils.isNullOrEmpty(hyperlinkUri)) return hyperlinkUri;

        LinkedHashSet<CharSequence> urlSet = TermuxUrlUtils.extractUrls(wordAtLongPress);
        if (urlSet.isEmpty()) return null;
        return (String) urlSet.iterator().next();
    }

    public String getLongPressedUrl() {
        return mLongPressedUrl;
    }

    public static boolean shouldShowLongPressedUrlMenuItems(String longPressedUrl) {
        return !DataUtils.isNullOrEmpty(longPressedUrl);
    }

    public void clearLongPressedUrl() {
        mLongPressedUrl = null;
    }

    public void openLongPressedUrlInApp() {
        if (DataUtils.isNullOrEmpty(mLongPressedUrl)) return;
        openUrlInApp(mLongPressedUrl);
    }

    public void openLongPressedUrlInChrome() {
        if (DataUtils.isNullOrEmpty(mLongPressedUrl)) return;
        ShareUtils.openUrlInChrome(mActivity, mLongPressedUrl);
    }

    public void copyLongPressedUrlToClipboard() {
        if (DataUtils.isNullOrEmpty(mLongPressedUrl)) return;
        ShareUtils.copyTextToClipboard(mActivity, mLongPressedUrl, mActivity.getString(R.string.msg_select_url_copied_to_clipboard));
    }

    @Override
    public boolean onOpenSelectedUrlRequested(String url) {
        if (DataUtils.isNullOrEmpty(url)) return false;
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) return false;
        browserController.openUrlInNewTab(url);
        return true;
    }

    @Override
    public boolean isTapToOpenUrlEnabled() {
        return mActivity.getPreferences().isTapToOpenUrlEnabled();
    }



    @Override
    public boolean onCodePoint(final int codePoint, boolean ctrlDown, TerminalSession session) {
        recordUserInputForSession(session);
        if (mVirtualFnKeyDown) {
            VirtualFunctionKeyMapper.Result mapping = VirtualFunctionKeyMapper.map(codePoint);
            int resultingKeyCode = mapping.keyCode;
            int resultingCodePoint = mapping.codePoint;
            boolean altDown = mapping.altDown;
            switch (mapping.sideEffect) {
                case ADJUST_VOLUME:
                    AudioManager audio = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
                    audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);
                    break;
                case TOGGLE_TERMINAL_TOOLBAR:
                    mActivity.toggleTerminalToolbar();
                    mVirtualFnKeyDown=false; // force disable fn key down to restore keyboard input into terminal view, fixes termux/termux-app#1420
                    break;
                case NONE:
                default:
                    break;
            }

            if (resultingKeyCode != VirtualFunctionKeyMapper.NONE) {
                TerminalEmulator term = session.getEmulator();
                session.write(KeyHandler.getCode(resultingKeyCode, 0, term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode()));
            } else if (resultingCodePoint != VirtualFunctionKeyMapper.NONE) {
                session.writeCodePoint(altDown, resultingCodePoint);
            }
            return true;
        } else if (ctrlDown) {
            if (codePoint == 106 /* Ctrl+j or \n */ && !session.isRunning()) {
                mTermuxTerminalSessionActivityClient.removeFinishedSession(session);
                return true;
            }

            List<KeyboardShortcut> shortcuts = mSessionShortcuts;
            if (shortcuts != null && !shortcuts.isEmpty()) {
                int codePointLowerCase = Character.toLowerCase(codePoint);
                for (int i = shortcuts.size() - 1; i >= 0; i--) {
                    KeyboardShortcut shortcut = shortcuts.get(i);
                    if (codePointLowerCase == shortcut.codePoint) {
                        switch (shortcut.shortcutAction) {
                            case TermuxPropertyConstants.ACTION_SHORTCUT_CREATE_SESSION:
                                mTermuxTerminalSessionActivityClient.addNewSession(false, null);
                                return true;
                            case TermuxPropertyConstants.ACTION_SHORTCUT_NEXT_SESSION:
                                mTermuxTerminalSessionActivityClient.switchToSession(true);
                                return true;
                            case TermuxPropertyConstants.ACTION_SHORTCUT_PREVIOUS_SESSION:
                                mTermuxTerminalSessionActivityClient.switchToSession(false);
                                return true;
                            case TermuxPropertyConstants.ACTION_SHORTCUT_RENAME_SESSION:
                                mTermuxTerminalSessionActivityClient.renameSession(mActivity.getCurrentSession());
                                return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void recordUserInputForSession(@Nullable TerminalSession session) {
        if (session == null || session.mSessionName == null) return;
        SessionNewActivityStore store = mActivity.getSessionNewActivityStore();
        if (store == null) return;
        store.recordUserInput(session.mSessionName, System.currentTimeMillis());
    }

    /**
     * Set the terminal sessions shortcuts.
     */
    private void setSessionShortcuts() {
        mSessionShortcuts = new ArrayList<>();

        // The {@link TermuxPropertyConstants#MAP_SESSION_SHORTCUTS} stores the session shortcut key and action pair
        for (Map.Entry<String, Integer> entry : TermuxPropertyConstants.MAP_SESSION_SHORTCUTS.entrySet()) {
            // The mMap stores the code points for the session shortcuts while loading properties
            Integer codePoint = (Integer) mActivity.getProperties().getInternalPropertyValue(entry.getKey(), true);
            // If codePoint is null, then session shortcut did not exist in properties or was invalid
            // as parsed by {@link #getCodePointForSessionShortcuts(String,String)}
            // If codePoint is not null, then get the action for the MAP_SESSION_SHORTCUTS key and
            // add the code point to sessionShortcuts
            if (codePoint != null)
                mSessionShortcuts.add(new KeyboardShortcut(codePoint, entry.getValue()));
        }
    }





    public void changeFontSize(boolean increase) {
        mActivity.getPreferences().changeFontSize(increase);
        mActivity.getTerminalView().setTextSize(mActivity.getPreferences().getFontSize());
    }



    /**
     * Called when user requests the soft keyboard to be toggled via "KEYBOARD" toggle button in
     * drawer or extra keys, or with ctrl+alt+k hardware keyboard shortcut.
     */
    public void onToggleSoftKeyboardRequest() {
        // If soft keyboard toggle behaviour is enable/disabled
        if (mActivity.getProperties().shouldEnableDisableSoftKeyboardOnToggle()) {
            // If soft keyboard is visible
            if (!KeyboardUtils.areDisableSoftKeyboardFlagsSet(mActivity)) {
                Logger.logVerbose(LOG_TAG, "Disabling soft keyboard on toggle");
                mActivity.getPreferences().setSoftKeyboardEnabled(false);
                KeyboardUtils.disableSoftKeyboard(mActivity, mActivity.getTerminalView());
            } else {
                // Show with a delay, otherwise pressing keyboard toggle won't show the keyboard after
                // switching back from another app if keyboard was previously disabled by user.
                // Also request focus, since it wouldn't have been requested at startup by
                // setSoftKeyboardState if keyboard was disabled. #2112
                Logger.logVerbose(LOG_TAG, "Enabling soft keyboard on toggle");
                mActivity.getPreferences().setSoftKeyboardEnabled(true);
                KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);
                if(mShowSoftKeyboardWithDelayOnce) {
                    mShowSoftKeyboardWithDelayOnce = false;
                    mActivity.getTerminalView().postDelayed(getShowSoftKeyboardRunnable(), 500);
                    mActivity.getTerminalView().requestFocus();
                } else
                    KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
            }
        }
        // If soft keyboard toggle behaviour is show/hide
        else {
            // If soft keyboard is disabled by user for Termux
            if (!mActivity.getPreferences().isSoftKeyboardEnabled()) {
                Logger.logVerbose(LOG_TAG, "Maintaining disabled soft keyboard on toggle");
                KeyboardUtils.disableSoftKeyboard(mActivity, mActivity.getTerminalView());
            } else {
                Logger.logVerbose(LOG_TAG, "Showing/Hiding soft keyboard on toggle");
                KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);
                KeyboardUtils.toggleSoftKeyboard(mActivity);
            }
        }
    }

    public void setSoftKeyboardState(boolean isStartup, boolean isReloadTermuxProperties) {
        boolean noShowKeyboard = false;

        // Requesting terminal view focus is necessary regardless of if soft keyboard is to be
        // disabled or hidden at startup, otherwise if hardware keyboard is attached and user
        // starts typing on hardware keyboard without tapping on the terminal first, then a colour
        // tint will be added to the terminal as highlight for the focussed view. Test with a light
        // theme. For android 8.+, the "defaultFocusHighlightEnabled" attribute is also set to false
        // in TerminalView layout to fix the issue.

        // If soft keyboard is disabled by user for Termux (check function docs for Termux behaviour info)
        if (KeyboardUtils.shouldSoftKeyboardBeDisabled(mActivity,
            mActivity.getPreferences().isSoftKeyboardEnabled(),
            mActivity.getPreferences().isSoftKeyboardEnabledOnlyIfNoHardware())) {
            Logger.logVerbose(LOG_TAG, "Maintaining disabled soft keyboard");
            KeyboardUtils.disableSoftKeyboard(mActivity, mActivity.getTerminalView());
            mActivity.getTerminalView().requestFocus();
            noShowKeyboard = true;
            // Delay is only required if onCreate() is called like when Termux app is exited with
            // double back press, not when Termux app is switched back from another app and keyboard
            // toggle is pressed to enable keyboard
            if (isStartup && mActivity.isOnResumeAfterOnCreate())
                mShowSoftKeyboardWithDelayOnce = true;
        } else {
            // Set flag to automatically push up TerminalView when keyboard is opened instead of showing over it
            KeyboardUtils.setSoftInputModeAdjustResize(mActivity);

            // Clear any previous flags to disable soft keyboard in case setting updated
            KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);

            // If soft keyboard is to be hidden on startup
            if (isStartup && mActivity.getProperties().shouldSoftKeyboardBeHiddenOnStartup()) {
                Logger.logVerbose(LOG_TAG, "Hiding soft keyboard on startup");
                // Required to keep keyboard hidden when Termux app is switched back from another app
                KeyboardUtils.setSoftKeyboardAlwaysHiddenFlags(mActivity);

                KeyboardUtils.hideSoftKeyboard(mActivity, mActivity.getTerminalView());
                mActivity.getTerminalView().requestFocus();
                noShowKeyboard = true;
                // Required to keep keyboard hidden on app startup
                mShowSoftKeyboardIgnoreOnce = true;
            }
        }

        mActivity.getTerminalView().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                // Force show soft keyboard if TerminalView or toolbar text input view has
                // focus and close it if they don't
                boolean textInputViewHasFocus = false;
                final EditText textInputView =  mActivity.findViewById(R.id.terminal_toolbar_text_input);
                if (textInputView != null) textInputViewHasFocus = textInputView.hasFocus();

                if (hasFocus || textInputViewHasFocus) {
                    if (mShowSoftKeyboardIgnoreOnce) {
                        mShowSoftKeyboardIgnoreOnce = false; return;
                    }
                    Logger.logVerbose(LOG_TAG, "Showing soft keyboard on focus change");
                } else {
                    Logger.logVerbose(LOG_TAG, "Hiding soft keyboard on focus change");
                }

                KeyboardUtils.setSoftKeyboardVisibility(getShowSoftKeyboardRunnable(), mActivity, mActivity.getTerminalView(), hasFocus || textInputViewHasFocus);
            }
        });

        // Do not force show soft keyboard if termux-reload-settings command was run with hardware keyboard
        // or soft keyboard is to be hidden or is disabled
        if (!isReloadTermuxProperties && !noShowKeyboard) {
            // Request focus for TerminalView
            // Also show the keyboard, since onFocusChange will not be called if TerminalView already
            // had focus on startup to show the keyboard, like when opening url with context menu
            // "Select URL" long press and returning to Termux app with back button. This
            // will also show keyboard even if it was closed before opening url. #2111
            Logger.logVerbose(LOG_TAG, "Requesting TerminalView focus and showing soft keyboard");
            mActivity.getTerminalView().requestFocus();
            mActivity.getTerminalView().postDelayed(getShowSoftKeyboardRunnable(), 300);
        }
    }

    private Runnable getShowSoftKeyboardRunnable() {
        if (mShowSoftKeyboardRunnable == null) {
            mShowSoftKeyboardRunnable = () -> {
                KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
            };
        }
        return mShowSoftKeyboardRunnable;
    }



    public void setTerminalCursorBlinkerState(boolean start) {
        if (start) {
            // If set/update the cursor blinking rate is successful, then enable cursor blinker
            if (mActivity.getTerminalView().setTerminalCursorBlinkerRate(mActivity.getProperties().getTerminalCursorBlinkRate()))
                mActivity.getTerminalView().setTerminalCursorBlinkerState(true, true);
            else
                Logger.logError(LOG_TAG,"Failed to start cursor blinker");
        } else {
            // Disable cursor blinker
            mActivity.getTerminalView().setTerminalCursorBlinkerState(false, true);
        }
    }



    public void shareSessionTranscript() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return;

        String transcriptText = ShellUtils.getTerminalSessionTranscriptText(session, false, true);
        if (transcriptText == null) return;

        // See https://github.com/termux/termux-app/issues/1166.
        transcriptText = DataUtils.getTruncatedCommandOutput(transcriptText, DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES, false, true, false).trim();
        ShareUtils.shareText(mActivity, mActivity.getString(R.string.title_share_transcript),
            transcriptText, mActivity.getString(R.string.title_share_transcript_with));
    }

    public void shareSelectedText() {
        String selectedText = mActivity.getTerminalView().getStoredSelectedText();
        if (DataUtils.isNullOrEmpty(selectedText)) return;
        ShareUtils.shareText(mActivity, mActivity.getString(R.string.title_share_selected_text),
            selectedText, mActivity.getString(R.string.title_share_selected_text_with));
    }

    public void showUrlSelection() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return;

        String text = ShellUtils.getTerminalSessionTranscriptText(session, true, true);

        LinkedHashSet<CharSequence> urlSet = TermuxUrlUtils.extractUrls(text);
        if (urlSet.isEmpty()) {
            DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity).setMessage(R.string.title_select_url_none_found));
            return;
        }

        final CharSequence[] urls = urlSet.toArray(new CharSequence[0]);
        Collections.reverse(Arrays.asList(urls)); // Latest first.

        final AlertDialog dialog = new AlertDialog.Builder(mActivity).setItems(urls, (di, which) -> {
            String url = (String) urls[which];
            showUrlOpenChoice(url);
        }).setTitle(R.string.title_select_url_dialog).setCancelable(true).create();
        dialog.setCanceledOnTouchOutside(true);

        // Long press to copy URL to clipboard:
        dialog.setOnShowListener(di -> {
            ListView lv = dialog.getListView(); // this is a ListView with your "buds" in it
            lv.setOnItemLongClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                String url = (String) urls[position];
                ShareUtils.copyTextToClipboard(mActivity, url, mActivity.getString(R.string.msg_select_url_copied_to_clipboard));
                return true;
            });
        });

        dialog.show();
    }

    private void showUrlOpenChoice(String url) {
        CharSequence[] actions = {
            mActivity.getString(R.string.action_open_url_in_app),
            mActivity.getString(R.string.action_browser_open_in_chrome),
            mActivity.getString(R.string.action_create_session_from_url)
        };
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(mActivity)
            .setTitle(url)
            .setItems(actions, (di, which) -> {
                if (which == 0) {
                    openUrlInApp(url);
                } else if (which == 1) {
                    ShareUtils.openUrlInChrome(mActivity, url);
                } else {
                    mTermuxTerminalSessionActivityClient.addNewSessionApplyingAutosshConfig(url);
                }
            }));
    }

    private void openUrlInApp(String url) {
        TermuxBrowserController browserController = mActivity.getTermuxBrowserController();
        if (browserController == null) return;
        browserController.openUrlInNewTab(url);
    }

    public void doPaste() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return;
        if (!session.isRunning()) return;

        String text = ShareUtils.getTextStringFromClipboardIfSet(mActivity, true);
        if (text != null)
            session.getEmulator().paste(text);
    }

}
