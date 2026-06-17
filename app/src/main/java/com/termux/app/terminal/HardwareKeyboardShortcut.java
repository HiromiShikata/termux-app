package com.termux.app.terminal;

import android.view.KeyEvent;

/**
 * Resolves the action for a Ctrl+Alt hardware keyboard shortcut.
 *
 * Extracted as pure logic from {@link TermuxTerminalViewClient#onKeyDown} so the shortcut
 * mapping can be unit tested without instantiating the Android view client. The Ctrl+Alt
 * modifier check and the consumption of the event remain the caller's responsibility; this
 * class only decides which action a matched shortcut maps to. An unmatched key still yields
 * {@link Action#NONE} (the caller consumes the event regardless).
 */
public final class HardwareKeyboardShortcut {

    /** The action a Ctrl+Alt shortcut maps to. */
    public enum Action {
        /** No specific action matched. */
        NONE,
        SWITCH_TO_NEXT_SESSION,
        SWITCH_TO_PREVIOUS_SESSION,
        OPEN_DRAWER,
        CLOSE_DRAWER,
        TOGGLE_SOFT_KEYBOARD,
        SHOW_CONTEXT_MENU,
        RENAME_SESSION,
        CREATE_SESSION,
        SHOW_URL_SELECTION,
        PASTE,
        INCREASE_FONT_SIZE,
        DECREASE_FONT_SIZE,
        /** Switch to the session at {@link Result#sessionIndex}. */
        SWITCH_TO_SESSION_INDEX
    }

    /** The resolved shortcut. */
    public static final class Result {

        /** The matched action. */
        public final Action action;

        /**
         * The zero-based session index for {@link Action#SWITCH_TO_SESSION_INDEX}; {@code -1}
         * for every other action.
         */
        public final int sessionIndex;

        Result(Action action, int sessionIndex) {
            this.action = action;
            this.sessionIndex = sessionIndex;
        }
    }

    private HardwareKeyboardShortcut() {
    }

    /**
     * Resolve the action for a Ctrl+Alt shortcut.
     *
     * @param keyCode The pressed key code.
     * @param unicodeChar The unmodified unicode char of the key ({@code e.getUnicodeChar(0)}).
     * @param shiftedUnicodeChar The shifted unicode char of the key
     * ({@code e.getUnicodeChar(KeyEvent.META_SHIFT_ON)}); only consulted for the font size
     * increase shortcut, since '+' may require shift on some layouts.
     * @return The resolved {@link Result}.
     */
    public static Result decide(int keyCode, int unicodeChar, int shiftedUnicodeChar) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || unicodeChar == 'n' /* next */) {
            return of(Action.SWITCH_TO_NEXT_SESSION);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || unicodeChar == 'p' /* previous */) {
            return of(Action.SWITCH_TO_PREVIOUS_SESSION);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return of(Action.OPEN_DRAWER);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            return of(Action.CLOSE_DRAWER);
        } else if (unicodeChar == 'k' /* keyboard */) {
            return of(Action.TOGGLE_SOFT_KEYBOARD);
        } else if (unicodeChar == 'm' /* menu */) {
            return of(Action.SHOW_CONTEXT_MENU);
        } else if (unicodeChar == 'r' /* rename */) {
            return of(Action.RENAME_SESSION);
        } else if (unicodeChar == 'c' /* create */) {
            return of(Action.CREATE_SESSION);
        } else if (unicodeChar == 'u' /* urls */) {
            return of(Action.SHOW_URL_SELECTION);
        } else if (unicodeChar == 'v') {
            return of(Action.PASTE);
        } else if (unicodeChar == '+' || shiftedUnicodeChar == '+') {
            // We also check for the shifted char here since shift may be required to produce '+',
            // see https://github.com/termux/termux-api/issues/2
            return of(Action.INCREASE_FONT_SIZE);
        } else if (unicodeChar == '-') {
            return of(Action.DECREASE_FONT_SIZE);
        } else if (unicodeChar >= '1' && unicodeChar <= '9') {
            return new Result(Action.SWITCH_TO_SESSION_INDEX, unicodeChar - '1');
        }
        return of(Action.NONE);
    }

    private static Result of(Action action) {
        return new Result(action, -1);
    }
}
