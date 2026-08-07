package com.termux.shared.termux.settings.preferences;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.android.PackageUtils;
import com.termux.shared.settings.preferences.AppSharedPreferences;
import com.termux.shared.settings.preferences.SharedPreferenceUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.logger.Logger;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_APP;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TermuxAppSharedPreferences extends AppSharedPreferences {

    private int MIN_FONTSIZE;
    private int MAX_FONTSIZE;
    private int DEFAULT_FONTSIZE;

    private static final String LOG_TAG = "TermuxAppSharedPreferences";

    private TermuxAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                TermuxConstants.TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                TermuxConstants.TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));

        setFontVariables(context);
    }

    /**
     * Get {@link TermuxAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link TermuxConstants#TERMUX_PACKAGE_NAME}.
     * @return Returns the {@link TermuxAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static TermuxAppSharedPreferences build(@NonNull final Context context) {
        Context termuxPackageContext = PackageUtils.getContextForPackage(context, TermuxConstants.TERMUX_PACKAGE_NAME);
        if (termuxPackageContext == null)
            return null;
        else
            return new TermuxAppSharedPreferences(termuxPackageContext);
    }

    /**
     * Get {@link TermuxAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link TermuxConstants#TERMUX_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link TermuxAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static TermuxAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context termuxPackageContext = TermuxUtils.getContextForPackageOrExitApp(context, TermuxConstants.TERMUX_PACKAGE_NAME, exitAppOnError);
        if (termuxPackageContext == null)
            return null;
        else
            return new TermuxAppSharedPreferences(termuxPackageContext);
    }



    public boolean shouldShowTerminalToolbar() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_TERMINAL_TOOLBAR, TERMUX_APP.DEFAULT_VALUE_SHOW_TERMINAL_TOOLBAR);
    }

    public void setShowTerminalToolbar(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_TERMINAL_TOOLBAR, value, false);
    }

    public boolean toogleShowTerminalToolbar() {
        boolean currentValue = shouldShowTerminalToolbar();
        setShowTerminalToolbar(!currentValue);
        return !currentValue;
    }


    public boolean isTerminalMarginAdjustmentEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_MARGIN_ADJUSTMENT, TERMUX_APP.DEFAULT_TERMINAL_MARGIN_ADJUSTMENT);
    }

    public void setTerminalMarginAdjustment(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_MARGIN_ADJUSTMENT, value, false);
    }



    public boolean isTapToOpenUrlEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TAP_TO_OPEN_URL_ENABLED, TERMUX_APP.DEFAULT_VALUE_TAP_TO_OPEN_URL_ENABLED);
    }

    public void setTapToOpenUrlEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TAP_TO_OPEN_URL_ENABLED, value, false);
    }


    public boolean isSoftKeyboardEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED);
    }

    public void setSoftKeyboardEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED, value, false);
    }

    public boolean isSoftKeyboardEnabledOnlyIfNoHardware() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE, TERMUX_APP.DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE);
    }

    public void setSoftKeyboardEnabledOnlyIfNoHardware(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE, value, false);
    }

    public boolean isVolumeKeysSwitchSessionsEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_VOLUME_KEYS_SWITCH_SESSIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_VOLUME_KEYS_SWITCH_SESSIONS_ENABLED);
    }

    public void setVolumeKeysSwitchSessionsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_VOLUME_KEYS_SWITCH_SESSIONS_ENABLED, value, false);
    }

    public boolean isSpeakTagAutoReadEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SPEAK_TAG_AUTO_READ_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_SPEAK_TAG_AUTO_READ_ENABLED);
    }

    public void setSpeakTagAutoReadEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SPEAK_TAG_AUTO_READ_ENABLED, value, false);
    }

    public boolean isOpenTagAutoOpenEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_OPEN_TAG_AUTO_OPEN_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_OPEN_TAG_AUTO_OPEN_ENABLED);
    }

    public void setOpenTagAutoOpenEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_OPEN_TAG_AUTO_OPEN_ENABLED, value, false);
    }



    public boolean shouldKeepScreenOn() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_KEEP_SCREEN_ON, TERMUX_APP.DEFAULT_VALUE_KEEP_SCREEN_ON);
    }

    public void setKeepScreenOn(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_KEEP_SCREEN_ON, value, false);
    }


    public static int[] getDefaultFontSizes(Context context) {
        float dipInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        int[] sizes = new int[3];

        // This is a bit arbitrary and sub-optimal. We want to give a sensible default for minimum font size
        // to prevent invisible text due to zoom be mistake:
        sizes[1] = (int) (4f * dipInPixels); // min

        // http://www.google.com/design/spec/style/typography.html#typography-line-height
        int defaultFontSize = Math.round(12 * dipInPixels);
        // Make it divisible by 2 since that is the minimal adjustment step:
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        sizes[0] = defaultFontSize; // default

        sizes[2] = 256; // max

        return sizes;
    }

    public void setFontVariables(Context context) {
        int[] sizes = getDefaultFontSizes(context);

        DEFAULT_FONTSIZE = sizes[0];
        MIN_FONTSIZE = sizes[1];
        MAX_FONTSIZE = sizes[2];
    }

    public int getFontSize() {
        int fontSize = SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_FONTSIZE, DEFAULT_FONTSIZE);
        return DataUtils.clamp(fontSize, MIN_FONTSIZE, MAX_FONTSIZE);
    }

    public void setFontSize(int value) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_FONTSIZE, value, false);
    }

    public void changeFontSize(boolean increase) {
        int fontSize = getFontSize();

        fontSize += (increase ? 1 : -1) * 2;
        fontSize = Math.max(MIN_FONTSIZE, Math.min(fontSize, MAX_FONTSIZE));

        setFontSize(fontSize);
    }



    public String getCurrentSession() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_CURRENT_SESSION, null, true);
    }

    public void setCurrentSession(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_CURRENT_SESSION, value, false);
    }



    public int getLogLevel() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_LOG_LEVEL, logLevel, false);
    }



    public int getLastNotificationId() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_LAST_NOTIFICATION_ID, TERMUX_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID);
    }

    public void setLastNotificationId(int notificationId) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_LAST_NOTIFICATION_ID, notificationId, false);
    }


    public synchronized int getAndIncrementAppShellNumberSinceBoot() {
        // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
        return SharedPreferenceUtils.getAndIncrementInt(mSharedPreferences, TERMUX_APP.KEY_APP_SHELL_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT, false, Integer.MAX_VALUE);
    }

    public synchronized void resetAppShellNumberSinceBoot() {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_APP_SHELL_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT, true);
    }

    public synchronized int getAndIncrementTerminalSessionNumberSinceBoot() {
        // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
        return SharedPreferenceUtils.getAndIncrementInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT, false, Integer.MAX_VALUE);
    }

    public synchronized void resetTerminalSessionNumberSinceBoot() {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT, true);
    }


    public boolean isTerminalViewKeyLoggingEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, TERMUX_APP.DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED);
    }

    public void setTerminalViewKeyLoggingEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, value, false);
    }



    public boolean arePluginErrorNotificationsEnabled(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, TERMUX_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED);
        else
            return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED);
    }

    public void setPluginErrorNotificationsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, value, false);
    }



    public boolean areCrashReportNotificationsEnabled(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, TERMUX_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED);
       else
            return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED);
    }

    public void setCrashReportNotificationsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, value, false);
    }



    public String getAutosshCommand() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_AUTOSSH_COMMAND, TERMUX_APP.DEFAULT_VALUE_KEY_AUTOSSH_COMMAND, false);
    }

    public void setAutosshCommand(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_AUTOSSH_COMMAND, value, false);
    }


    public String getResetSessionCommand() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_RESET_SESSION_COMMAND, TERMUX_APP.DEFAULT_VALUE_KEY_RESET_SESSION_COMMAND, false);
    }

    public void setResetSessionCommand(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_RESET_SESSION_COMMAND, value, false);
    }


    public String getKillSessionCommand() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_KILL_SESSION_COMMAND, TERMUX_APP.DEFAULT_VALUE_KEY_KILL_SESSION_COMMAND, false);
    }

    public void setKillSessionCommand(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_KILL_SESSION_COMMAND, value, false);
    }


    public String getSessionDefinitionUrl() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_URL, TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_URL, false);
    }

    public void setSessionDefinitionUrl(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_URL, value, false);
    }


    public int getSessionDefinitionReloadIntervalMinutes() {
        int value = SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES, TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES);
        return value < 0 ? 0 : value;
    }

    public void setSessionDefinitionReloadIntervalMinutes(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES, value < 0 ? 0 : value, false);
    }


    public int getBackgroundReconnectScanIntervalMinutes() {
        int value = SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_BACKGROUND_RECONNECT_SCAN_INTERVAL_MINUTES, TERMUX_APP.DEFAULT_VALUE_KEY_BACKGROUND_RECONNECT_SCAN_INTERVAL_MINUTES);
        return value < 0 ? 0 : value;
    }

    public void setBackgroundReconnectScanIntervalMinutes(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_BACKGROUND_RECONNECT_SCAN_INTERVAL_MINUTES, value < 0 ? 0 : value, false);
    }


    public int getSessionDefinitionMaxSessions() {
        int value = SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_MAX_SESSIONS, TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS);
        return value < TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS
            ? TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS : value;
    }

    public void setSessionDefinitionMaxSessions(int value) {
        int sanitized = value < TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS
            ? TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS : value;
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_MAX_SESSIONS, sanitized, false);
    }


    public boolean shouldRemoveGithubSessionsNotInList() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_REMOVE_GITHUB_SESSIONS_NOT_IN_LIST, TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_REMOVE_GITHUB_SESSIONS_NOT_IN_LIST);
    }

    public void setRemoveGithubSessionsNotInList(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SESSION_DEFINITION_REMOVE_GITHUB_SESSIONS_NOT_IN_LIST, value, false);
    }


    public String getAlwaysNaSessionNamesText() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_ALWAYS_NA_SESSION_NAMES, TERMUX_APP.DEFAULT_VALUE_KEY_ALWAYS_NA_SESSION_NAMES, false);
    }

    public void setAlwaysNaSessionNames(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_ALWAYS_NA_SESSION_NAMES, value, false);
    }

    @NonNull
    public Set<String> getAlwaysNaSessionNames() {
        return parseAlwaysNaSessionNames(getAlwaysNaSessionNamesText());
    }

    @NonNull
    public static Set<String> parseAlwaysNaSessionNames(@Nullable String value) {
        Set<String> names = new LinkedHashSet<>();
        if (value == null) {
            return names;
        }
        for (String line : value.split("\n")) {
            String trimmedName = line.trim();
            if (!trimmedName.isEmpty()) {
                names.add(trimmedName);
            }
        }
        return names;
    }


    public String getCollapsedProjectKeysText() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_COLLAPSED_PROJECT_KEYS, TERMUX_APP.DEFAULT_VALUE_KEY_COLLAPSED_PROJECT_KEYS, false);
    }

    public void setCollapsedProjectKeys(@NonNull Set<String> collapsedProjectKeys) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_COLLAPSED_PROJECT_KEYS, serializeCollapsedProjectKeys(collapsedProjectKeys), false);
    }

    @NonNull
    public Set<String> getCollapsedProjectKeys() {
        return parseCollapsedProjectKeys(getCollapsedProjectKeysText());
    }

    @NonNull
    public static Set<String> parseCollapsedProjectKeys(@Nullable String value) {
        Set<String> keys = new LinkedHashSet<>();
        if (value == null) {
            return keys;
        }
        for (String line : value.split("\n")) {
            String trimmedKey = line.trim();
            if (!trimmedKey.isEmpty()) {
                keys.add(trimmedKey);
            }
        }
        return keys;
    }

    public String getPinnedTextInputHistory() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_PINNED_TEXT_INPUT_HISTORY, TERMUX_APP.DEFAULT_VALUE_KEY_PINNED_TEXT_INPUT_HISTORY, false);
    }

    public void setPinnedTextInputHistory(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_PINNED_TEXT_INPUT_HISTORY, value, false);
    }

    @NonNull
    public static String serializeCollapsedProjectKeys(@NonNull Set<String> collapsedProjectKeys) {
        StringBuilder serialized = new StringBuilder();
        for (String collapsedProjectKey : collapsedProjectKeys) {
            if (collapsedProjectKey == null) {
                continue;
            }
            String trimmedKey = collapsedProjectKey.trim();
            if (trimmedKey.isEmpty()) {
                continue;
            }
            if (serialized.length() > 0) {
                serialized.append("\n");
            }
            serialized.append(trimmedKey);
        }
        return serialized.toString();
    }


    public boolean isSessionSwitchPreviewFirstEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SESSION_SWITCH_PREVIEW_FIRST, TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_SWITCH_PREVIEW_FIRST);
    }

    public void setSessionSwitchPreviewFirstEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SESSION_SWITCH_PREVIEW_FIRST, value, false);
    }

    public boolean isSessionSwitchOverlayEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SESSION_SWITCH_OVERLAY_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_SWITCH_OVERLAY_ENABLED);
    }

    public void setSessionSwitchOverlayEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SESSION_SWITCH_OVERLAY_ENABLED, value, false);
    }


    public String getDisabledSessionNamesText() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_DISABLED_SESSION_NAMES, TERMUX_APP.DEFAULT_VALUE_KEY_DISABLED_SESSION_NAMES, false);
    }

    public void setDisabledSessionNames(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_DISABLED_SESSION_NAMES, value, false);
    }

    @NonNull
    public Set<String> getDisabledSessionNames() {
        return parseDisabledSessionNames(getDisabledSessionNamesText());
    }

    @NonNull
    public static Set<String> parseDisabledSessionNames(@Nullable String value) {
        Set<String> names = new LinkedHashSet<>();
        if (value == null) {
            return names;
        }
        for (String line : value.split("\n")) {
            String trimmedName = line.trim();
            if (!trimmedName.isEmpty()) {
                names.add(trimmedName);
            }
        }
        return names;
    }

    @NonNull
    public static String serializeDisabledSessionNames(@NonNull Set<String> names) {
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(name.trim());
        }
        return builder.toString();
    }

    public boolean isSessionDisabled(@Nullable String sessionName) {
        return HiddenSessionNameMatcher.matchesAHiddenSession(sessionName, getDisabledSessionNames());
    }

    public boolean setSessionDisabled(@Nullable String sessionName, boolean disabled) {
        if (sessionName == null || sessionName.isEmpty()) {
            return false;
        }
        Set<String> names = getDisabledSessionNames();
        if (disabled) {
            names.add(sessionName);
        } else {
            names.remove(sessionName);
        }
        setDisabledSessionNames(serializeDisabledSessionNames(names));
        return disabled;
    }


    public String getUserRemovedSessionNamesText() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_USER_REMOVED_SESSION_NAMES, TERMUX_APP.DEFAULT_VALUE_KEY_USER_REMOVED_SESSION_NAMES, false);
    }

    public void setUserRemovedSessionNames(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_USER_REMOVED_SESSION_NAMES, value, false);
    }

    @NonNull
    public Set<String> getUserRemovedSessionNames() {
        return parseDisabledSessionNames(getUserRemovedSessionNamesText());
    }

    public boolean isSessionUserRemoved(@Nullable String sessionName) {
        return sessionName != null && getUserRemovedSessionNames().contains(sessionName);
    }

    public void setSessionUserRemoved(@Nullable String sessionName, boolean removed) {
        if (sessionName == null || sessionName.isEmpty()) {
            return;
        }
        Set<String> names = getUserRemovedSessionNames();
        boolean changed = removed ? names.add(sessionName) : names.remove(sessionName);
        if (changed) {
            setUserRemovedSessionNames(serializeDisabledSessionNames(names));
        }
    }


    public String getUserRemovedSessionTimesText() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_USER_REMOVED_SESSION_TIMES, TERMUX_APP.DEFAULT_VALUE_KEY_USER_REMOVED_SESSION_TIMES, false);
    }

    public void setUserRemovedSessionTimes(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_USER_REMOVED_SESSION_TIMES, value, false);
    }

    @NonNull
    public Map<String, Long> getUserRemovedSessionTimes() {
        return UserRemovedSessionTimeSerializer.parse(getUserRemovedSessionTimesText());
    }

    public void recordSessionUserRemovedAt(@Nullable String sessionName, long removedAtMillis) {
        if (sessionName == null || sessionName.isEmpty()) {
            return;
        }
        Map<String, Long> removalTimes = getUserRemovedSessionTimes();
        Iterator<Map.Entry<String, Long>> removalRecords = removalTimes.entrySet().iterator();
        while (removalRecords.hasNext()) {
            if (!UserRemovedSessionHideWindow.hidesSession(removalRecords.next().getValue(), removedAtMillis)) {
                removalRecords.remove();
            }
        }
        removalTimes.put(sessionName, removedAtMillis);
        setUserRemovedSessionTimes(UserRemovedSessionTimeSerializer.serialize(removalTimes));
    }

    public void clearSessionUserRemovedTime(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return;
        }
        Map<String, Long> removalTimes = getUserRemovedSessionTimes();
        if (removalTimes.remove(sessionName) == null) {
            return;
        }
        setUserRemovedSessionTimes(UserRemovedSessionTimeSerializer.serialize(removalTimes));
    }


    public boolean shouldHideHiddenSessions() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_HIDE_HIDDEN_SESSIONS, TERMUX_APP.DEFAULT_VALUE_KEY_HIDE_HIDDEN_SESSIONS);
    }

    public void setHideHiddenSessions(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_HIDE_HIDDEN_SESSIONS, value, false);
    }

    public boolean toggleHideHiddenSessions() {
        boolean newValue = !shouldHideHiddenSessions();
        setHideHiddenSessions(newValue);
        return newValue;
    }


    public String getPersistedSessions() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_PERSISTED_SESSIONS, TERMUX_APP.DEFAULT_VALUE_KEY_PERSISTED_SESSIONS, false);
    }

    public void setPersistedSessions(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_PERSISTED_SESSIONS, value, false);
    }


    public String getPersistedSessionNewActivityStates() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_PERSISTED_SESSION_NEW_ACTIVITY_STATES, TERMUX_APP.DEFAULT_VALUE_KEY_PERSISTED_SESSION_NEW_ACTIVITY_STATES, false);
    }

    public void setPersistedSessionNewActivityStates(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_PERSISTED_SESSION_NEW_ACTIVITY_STATES, value, false);
    }


    public String getBrowserBookmarks() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_BOOKMARKS, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_BOOKMARKS, false);
    }

    public void setBrowserBookmarks(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_BOOKMARKS, value, false);
    }



    public String getBrowserOpenSessionNames() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_OPEN_SESSION_NAMES, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_OPEN_SESSION_NAMES, false);
    }

    public void setBrowserOpenSessionNames(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_OPEN_SESSION_NAMES, value, false);
    }

    public String getBrowserSessionTabs() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_SESSION_TABS, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_SESSION_TABS, false);
    }

    public void setBrowserSessionTabs(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_SESSION_TABS, value, false);
    }

    public String getBrowserSessionSplitRatios() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_SESSION_SPLIT_RATIOS, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_SESSION_SPLIT_RATIOS, false);
    }

    public void setBrowserSessionSplitRatios(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_SESSION_SPLIT_RATIOS, value, false);
    }

    public String getBrowserTabHistory() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_TAB_HISTORY, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_TAB_HISTORY, false);
    }

    public void setBrowserTabHistory(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_TAB_HISTORY, value, false);
    }


    public boolean isBrowserMeetLowPowerVideoEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED);
    }

    public void setBrowserMeetLowPowerVideoEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED, value, false);
    }

    public int getBrowserMeetLowPowerVideoMaxWidth() {
        return SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_WIDTH, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_WIDTH);
    }

    public void setBrowserMeetLowPowerVideoMaxWidth(int value) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_WIDTH, value, false);
    }

    public int getBrowserMeetLowPowerVideoMaxHeight() {
        return SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_HEIGHT, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_HEIGHT);
    }

    public void setBrowserMeetLowPowerVideoMaxHeight(int value) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_HEIGHT, value, false);
    }

    public int getBrowserMeetLowPowerVideoMaxFramerate() {
        return SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_FRAMERATE, TERMUX_APP.DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_FRAMERATE);
    }

    public void setBrowserMeetLowPowerVideoMaxFramerate(int value) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_FRAMERATE, value, false);
    }

}
