package com.termux.shared.termux.settings.preferences;

/*
 * Version: v0.24.0
 *
 * Changelog
 *
 * - 0.1.0 (2021-03-12)
 *      - Initial Release.
 *
 * - 0.2.0 (2021-03-13)
 *      - Added `KEY_LOG_LEVEL` and `KEY_TERMINAL_VIEW_LOGGING_ENABLED`.
 *
 * - 0.3.0 (2021-03-16)
 *      - Changed to per app scoping of variables so that the same file can store all constants of
 *          Termux app and its plugins. This will allow {@link com.termux.app.TermuxSettings} to
 *          manage preferences of plugins as well if they don't have launcher activity themselves
 *          and also allow plugin apps to make changes to preferences from background.
 *      - Added following to `TERMUX_TASKER_APP`:
 *           `KEY_LOG_LEVEL`.
 *
 * - 0.4.0 (2021-03-13)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED` and `DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED`.
 *
 * - 0.5.0 (2021-03-24)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_LAST_NOTIFICATION_ID` and `DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID`.
 *
 * - 0.6.0 (2021-03-24)
 *      - Change `DEFAULT_VALUE_KEEP_SCREEN_ON` value to `false` in `TERMUX_APP`.
 *
 * - 0.7.0 (2021-03-27)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_SOFT_KEYBOARD_ENABLED` and `DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED`.
 *
 * - 0.8.0 (2021-04-06)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED` and `DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED`.
 *
 * - 0.9.0 (2021-04-07)
 *      - Updated javadocs.
 *
 * - 0.10.0 (2021-05-12)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE` and `DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE`.
 *
 * - 0.11.0 (2021-07-08)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_DISABLE_TERMINAL_MARGIN_ADJUSTMENT`.
 *
 * - 0.12.0 (2021-08-27)
 *      - Added `TERMUX_API_APP.KEY_LOG_LEVEL`, `TERMUX_BOOT_APP.KEY_LOG_LEVEL`,
 *          `TERMUX_FLOAT_APP.KEY_LOG_LEVEL`, `TERMUX_STYLING_APP.KEY_LOG_LEVEL`,
 *          `TERMUX_Widget_APP.KEY_LOG_LEVEL`.
 *
 * - 0.13.0 (2021-09-02)
 *      - Added following to `TERMUX_FLOAT_APP`:
 *          `KEY_WINDOW_X`, `KEY_WINDOW_Y`, `KEY_WINDOW_WIDTH`, `KEY_WINDOW_HEIGHT`, `KEY_FONTSIZE`,
 *          `KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED`.
 *
 * - 0.14.0 (2021-09-04)
 *      - Added `TERMUX_WIDGET_APP.KEY_TOKEN`.
 *
 * - 0.15.0 (2021-09-05)
 *      - Added following to `TERMUX_TASKER_APP`:
 *          `KEY_LAST_PENDING_INTENT_REQUEST_CODE` and `DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE`.
 *
 * - 0.16.0 (2022-06-11)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_APP_SHELL_NUMBER_SINCE_BOOT` and `KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT`.
 *
 * - 0.17.0 (2026-06-11)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_AUTOSSH_COMMAND` and `DEFAULT_VALUE_KEY_AUTOSSH_COMMAND`.
 *
 * - 0.18.0 (2026-06-13)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_PERSISTED_SESSIONS` and `DEFAULT_VALUE_KEY_PERSISTED_SESSIONS`.
 *
 * - 0.19.0 (2026-06-14)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_BROWSER_SPLIT_RATIO` and `DEFAULT_VALUE_BROWSER_SPLIT_RATIO`.
 *
 * - 0.20.0 (2026-06-14)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_SPEAK_TAG_AUTO_READ_ENABLED` and `DEFAULT_VALUE_KEY_SPEAK_TAG_AUTO_READ_ENABLED`.
 *
 * - 0.21.0 (2026-06-14)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_OPEN_TAG_AUTO_OPEN_ENABLED` and `DEFAULT_VALUE_KEY_OPEN_TAG_AUTO_OPEN_ENABLED`.
 *
 * - 0.22.0 (2026-06-15)
 *      - Removed following from `TERMUX_APP`:
 *          `KEY_BROWSER_SPLIT_RATIO` and `DEFAULT_VALUE_BROWSER_SPLIT_RATIO`. The in-app browser
 *          split height now resets to its two-thirds default on each open instead of being persisted.
 *
 * - 0.23.0 (2026-06-21)
 *      - Changed `DEFAULT_VALUE_KEY_OPEN_TAG_AUTO_OPEN_ENABLED` from `false` to `true` so that
 *          `http`/`https` URLs inside `<open>...</open>` tags auto-open in the in-app browser out of
 *          the box. The behavior remains user-configurable via the existing settings toggle.
 *
 * - 0.24.0 (2026-06-24)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_BROWSER_OPEN_SESSION_NAMES` and `DEFAULT_VALUE_KEY_BROWSER_OPEN_SESSION_NAMES`, which
 *          persist the set of session names whose in-app browser area was left open so the open state
 *          is restored when returning to a session and after an app restart.
 *
 * - 0.25.0 (2026-06-30)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_BROWSER_SESSION_SPLIT_RATIOS` and `DEFAULT_VALUE_KEY_BROWSER_SESSION_SPLIT_RATIOS`,
 *          which persist the in-app browser split height keyed by session name so each session
 *          restores its own browser height when returning to it and after an app restart.
 *
 * - 0.26.0 (2026-07-29)
 *      - Added following to `TERMUX_APP`:
 *          `KEY_RESET_SESSION_COMMAND` and `DEFAULT_VALUE_KEY_RESET_SESSION_COMMAND`, which hold the
 *          user-configurable command template run to reset a host session, with `{name}` substituted
 *          by the shell-quoted host session name.
 */

import com.termux.shared.shell.command.ExecutionCommand;

/**
 * A class that defines shared constants of the SharedPreferences used by Termux app and its plugins.
 * This class will be hosted by termux-shared lib and should be imported by other termux plugin
 * apps as is instead of copying constants to random classes. The 3rd party apps can also import
 * it for interacting with termux apps. If changes are made to this file, increment the version number
 * and add an entry in the Changelog section above.
 */
public final class TermuxPreferenceConstants {

    /**
     * Termux app constants.
     */
    public static final class TERMUX_APP {

        /**
         * Defines the key for whether terminal view margin adjustment that is done to prevent soft
         * keyboard from covering bottom part of terminal view on some devices is enabled or not.
         * Margin adjustment may cause screen flickering on some devices and so should be disabled.
         */
        public static final String KEY_TERMINAL_MARGIN_ADJUSTMENT =  "terminal_margin_adjustment";
        public static final boolean DEFAULT_TERMINAL_MARGIN_ADJUSTMENT = true;


        /**
         * Defines the key for whether to show terminal toolbar containing extra keys and text input field.
         */
        public static final String KEY_SHOW_TERMINAL_TOOLBAR = "show_extra_keys";
        public static final boolean DEFAULT_VALUE_SHOW_TERMINAL_TOOLBAR = true;


        /**
         * Defines the key for whether a single tap on a terminal cell that is on a URL opens that
         * URL in the in-app browser, taking precedence over forwarding the tap to a program with
         * mouse tracking active.
         */
        public static final String KEY_TAP_TO_OPEN_URL_ENABLED = "tap_to_open_url_enabled";
        public static final boolean DEFAULT_VALUE_TAP_TO_OPEN_URL_ENABLED = true;


        /**
         * Defines the key for whether the content inside `<speak>...</speak>` tags that appears in the
         * terminal output is automatically read aloud via Android text-to-speech.
         */
        public static final String KEY_SPEAK_TAG_AUTO_READ_ENABLED = "speak_tag_auto_read_enabled";
        public static final boolean DEFAULT_VALUE_KEY_SPEAK_TAG_AUTO_READ_ENABLED = false;


        /**
         * Defines the key for whether an `http`/`https` URL inside `<open>...</open>` tags that appears
         * in the terminal output is automatically opened in the in-app browser.
         */
        public static final String KEY_OPEN_TAG_AUTO_OPEN_ENABLED = "open_tag_auto_open_enabled";
        public static final boolean DEFAULT_VALUE_KEY_OPEN_TAG_AUTO_OPEN_ENABLED = true;


        /**
         * Defines the key for whether the soft keyboard will be enabled, for cases where users want
         * to use a hardware keyboard instead.
         */
        public static final String KEY_SOFT_KEYBOARD_ENABLED = "soft_keyboard_enabled";
        public static final boolean DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED = true;

        /**
         * Defines the key for whether the soft keyboard will be enabled only if no hardware keyboard
         * attached, for cases where users want to use a hardware keyboard instead.
         */
        public static final String KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE = "soft_keyboard_enabled_only_if_no_hardware";
        public static final boolean DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE = false;

        /**
         * Defines the key for whether the hardware volume keys switch terminal sessions while the
         * app is focused, overriding system volume control and the virtual volume-key modifiers.
         */
        public static final String KEY_VOLUME_KEYS_SWITCH_SESSIONS_ENABLED = "volume_keys_switch_sessions_enabled";
        public static final boolean DEFAULT_VALUE_KEY_VOLUME_KEYS_SWITCH_SESSIONS_ENABLED = true;


        /**
         * Defines the key for whether to always keep screen on.
         */
        public static final String KEY_KEEP_SCREEN_ON = "screen_always_on";
        public static final boolean DEFAULT_VALUE_KEEP_SCREEN_ON = false;


        /**
         * Defines the key for font size of termux terminal view.
         */
        public static final String KEY_FONTSIZE = "fontsize";


        /**
         * Defines the key for current termux terminal session.
         */
        public static final String KEY_CURRENT_SESSION = "current_session";


        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";


        /**
         * Defines the key for last used notification id.
         */
        public static final String KEY_LAST_NOTIFICATION_ID = "last_notification_id";
        public static final int DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID = 0;

        public static final String KEY_AUTOSSH_COMMAND = "autossh_command";
        public static final String DEFAULT_VALUE_KEY_AUTOSSH_COMMAND = "";

        public static final String KEY_RESET_SESSION_COMMAND = "reset_session_command";
        public static final String DEFAULT_VALUE_KEY_RESET_SESSION_COMMAND = "";

        public static final String KEY_KILL_SESSION_COMMAND = "kill_session_command";
        public static final String DEFAULT_VALUE_KEY_KILL_SESSION_COMMAND = "";

        public static final String KEY_SESSION_DEFINITION_URL = "session_definition_url";
        public static final String DEFAULT_VALUE_KEY_SESSION_DEFINITION_URL = "";

        public static final String KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES = "session_definition_reload_interval_minutes";
        public static final int DEFAULT_VALUE_KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES = 0;

        public static final String KEY_BACKGROUND_RECONNECT_SCAN_INTERVAL_MINUTES = "background_reconnect_scan_interval_minutes";
        public static final int DEFAULT_VALUE_KEY_BACKGROUND_RECONNECT_SCAN_INTERVAL_MINUTES = 5;

        public static final String KEY_SESSION_DEFINITION_MAX_SESSIONS = "session_definition_max_sessions";
        public static final int DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS = 64;
        public static final int MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS = 1;

        public static final String KEY_SESSION_DEFINITION_REMOVE_GITHUB_SESSIONS_NOT_IN_LIST = "session_definition_remove_github_sessions_not_in_list";
        public static final boolean DEFAULT_VALUE_KEY_SESSION_DEFINITION_REMOVE_GITHUB_SESSIONS_NOT_IN_LIST = true;

        public static final String KEY_ALWAYS_NA_SESSION_NAMES = "always_na_session_names";
        public static final String DEFAULT_VALUE_KEY_ALWAYS_NA_SESSION_NAMES = "";

        public static final String KEY_COLLAPSED_PROJECT_KEYS = "collapsed_project_keys";
        public static final String DEFAULT_VALUE_KEY_COLLAPSED_PROJECT_KEYS = "";

        public static final String KEY_PINNED_TEXT_INPUT_HISTORY = "pinned_text_input_history";
        public static final String DEFAULT_VALUE_KEY_PINNED_TEXT_INPUT_HISTORY = "";

        public static final String KEY_SESSION_SWITCH_PREVIEW_FIRST = "session_switch_preview_first";
        public static final boolean DEFAULT_VALUE_KEY_SESSION_SWITCH_PREVIEW_FIRST = false;

        public static final String KEY_SESSION_SWITCH_OVERLAY_ENABLED = "session_switch_overlay_enabled";
        public static final boolean DEFAULT_VALUE_KEY_SESSION_SWITCH_OVERLAY_ENABLED = false;

        public static final String KEY_DISABLED_SESSION_NAMES = "disabled_session_names";
        public static final String DEFAULT_VALUE_KEY_DISABLED_SESSION_NAMES = "";

        public static final String KEY_USER_REMOVED_SESSION_NAMES = "user_removed_session_names";
        public static final String DEFAULT_VALUE_KEY_USER_REMOVED_SESSION_NAMES = "";

        public static final String KEY_USER_REMOVED_SESSION_TIMES = "user_removed_session_times";
        public static final String DEFAULT_VALUE_KEY_USER_REMOVED_SESSION_TIMES = "";

        public static final String KEY_HIDE_HIDDEN_SESSIONS = "hide_hidden_sessions";
        public static final boolean DEFAULT_VALUE_KEY_HIDE_HIDDEN_SESSIONS = false;

        public static final String KEY_PERSISTED_SESSIONS = "persisted_sessions";
        public static final String DEFAULT_VALUE_KEY_PERSISTED_SESSIONS = "";

        public static final String KEY_PERSISTED_SESSION_NEW_ACTIVITY_STATES = "persisted_session_new_activity_states";
        public static final String DEFAULT_VALUE_KEY_PERSISTED_SESSION_NEW_ACTIVITY_STATES = "";

        public static final String KEY_BROWSER_BOOKMARKS = "browser_bookmarks";
        public static final String DEFAULT_VALUE_KEY_BROWSER_BOOKMARKS = "";

        public static final String KEY_BROWSER_OPEN_SESSION_NAMES = "browser_open_session_names";
        public static final String DEFAULT_VALUE_KEY_BROWSER_OPEN_SESSION_NAMES = "";

        public static final String KEY_BROWSER_SESSION_TABS = "browser_session_tabs";
        public static final String DEFAULT_VALUE_KEY_BROWSER_SESSION_TABS = "";

        public static final String KEY_BROWSER_SESSION_SPLIT_RATIOS = "browser_session_split_ratios";
        public static final String DEFAULT_VALUE_KEY_BROWSER_SESSION_SPLIT_RATIOS = "";

        public static final String KEY_BROWSER_TAB_HISTORY = "browser_tab_history";
        public static final String DEFAULT_VALUE_KEY_BROWSER_TAB_HISTORY = "";

        public static final String KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED = "browser_meet_low_power_video_enabled";
        public static final boolean DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_ENABLED = false;

        public static final String KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_WIDTH = "browser_meet_low_power_video_max_width";
        public static final int DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_WIDTH = 640;

        public static final String KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_HEIGHT = "browser_meet_low_power_video_max_height";
        public static final int DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_HEIGHT = 360;

        public static final String KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_FRAMERATE = "browser_meet_low_power_video_max_framerate";
        public static final int DEFAULT_VALUE_KEY_BROWSER_MEET_LOW_POWER_VIDEO_MAX_FRAMERATE = 15;

        /**
         * The {@link ExecutionCommand.Runner#APP_SHELL} number after termux app process since boot.
         */
        public static final String KEY_APP_SHELL_NUMBER_SINCE_BOOT = "app_shell_number_since_boot";
        public static final int DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT = 0;

        /**
         * The {@link ExecutionCommand.Runner#TERMINAL_SESSION} number after termux app process since boot.
         */
        public static final String KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT = "terminal_session_number_since_boot";
        public static final int DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT = 0;


        /**
         * Defines the key for whether termux terminal view key logging is enabled or not
         */
        public static final String KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED = "terminal_view_key_logging_enabled";
        public static final boolean DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED = false;

        /**
         * Defines the key for whether flashes and notifications for plugin errors are enabled or not.
         */
        public static final String KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED = "plugin_error_notifications_enabled";
        public static final boolean DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED = true;

        /**
         * Defines the key for whether notifications for crash reports are enabled or not.
         */
        public static final String KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED = "crash_report_notifications_enabled";
        public static final boolean DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED = true;

    }



    /**
     * Termux:API app constants.
     */
    public static final class TERMUX_API_APP {

        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";


        /**
         * Defines the key for last used PendingIntent request code.
         */
        public static final String KEY_LAST_PENDING_INTENT_REQUEST_CODE = "last_pending_intent_request_code";
        public static final int DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE = 0;

    }



    /**
     * Termux:Boot app constants.
     */
    public static final class TERMUX_BOOT_APP {

        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";

    }



    /**
     * Termux:Float app constants.
     */
    public static final class TERMUX_FLOAT_APP {

        /**
         * The float window x coordinate.
         */
        public static final String KEY_WINDOW_X = "window_x";

        /**
         * The float window y coordinate.
         */
        public static final String KEY_WINDOW_Y = "window_y";

        /**
         * The float window width.
         */
        public static final String KEY_WINDOW_WIDTH = "window_width";

        /**
         * The float window height.
         */
        public static final String KEY_WINDOW_HEIGHT = "window_height";

        /**
         * Defines the key for font size of termux terminal view.
         */
        public static final String KEY_FONTSIZE = "fontsize";

        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";

        /**
         * Defines the key for whether termux terminal view key logging is enabled or not
         */
        public static final String KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED = "terminal_view_key_logging_enabled";
        public static final boolean DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED = false;

    }



    /**
     * Termux:Styling app constants.
     */
    public static final class TERMUX_STYLING_APP {

        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";

    }



    /**
     * Termux:Tasker app constants.
     */
    public static final class TERMUX_TASKER_APP {

        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";


        /**
         * Defines the key for last used PendingIntent request code.
         */
        public static final String KEY_LAST_PENDING_INTENT_REQUEST_CODE = "last_pending_intent_request_code";
        public static final int DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE = 0;

    }



    /**
     * Termux:Widget app constants.
     */
    public static final class TERMUX_WIDGET_APP {

        /**
         * Defines the key for current log level.
         */
        public static final String KEY_LOG_LEVEL = "log_level";

        /**
         * Defines the key for current token for shortcuts.
         */
        public static final String KEY_TOKEN = "token";

    }

}
