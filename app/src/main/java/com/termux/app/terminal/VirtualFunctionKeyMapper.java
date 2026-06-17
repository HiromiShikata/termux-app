package com.termux.app.terminal;

import android.view.KeyEvent;

/**
 * Maps a code point typed while the virtual function key (Volume Up held) is down to the
 * resulting terminal action.
 *
 * Extracted as pure logic from {@link TermuxTerminalViewClient#onCodePoint} so the mapping
 * table can be unit tested without instantiating the Android view client. The behaviour is
 * identical to the original inline {@code switch} statement; the side effects (adjusting the
 * media volume and toggling the terminal toolbar) are returned as a {@link SideEffect} value
 * for the caller to perform.
 */
public final class VirtualFunctionKeyMapper {

    /** Sentinel meaning "no key code" / "no code point". */
    public static final int NONE = -1;

    /** A side effect the caller must perform after consulting the mapping. */
    public enum SideEffect {
        /** No side effect. */
        NONE,
        /** Adjust the suggested media stream volume and show the volume UI. */
        ADJUST_VOLUME,
        /** Toggle the terminal toolbar and release the virtual function key. */
        TOGGLE_TERMINAL_TOOLBAR
    }

    /** The resolved mapping for a code point. */
    public static final class Result {

        /** The {@link KeyEvent} key code to send to the session, or {@link #NONE}. */
        public final int keyCode;

        /** The code point to write to the session, or {@link #NONE}. */
        public final int codePoint;

        /** Whether {@link #codePoint} should be written with the alt modifier held. */
        public final boolean altDown;

        /** The side effect the caller must perform. */
        public final SideEffect sideEffect;

        Result(int keyCode, int codePoint, boolean altDown, SideEffect sideEffect) {
            this.keyCode = keyCode;
            this.codePoint = codePoint;
            this.altDown = altDown;
            this.sideEffect = sideEffect;
        }
    }

    private VirtualFunctionKeyMapper() {
    }

    /**
     * Resolve the mapping for a code point typed while the virtual function key is held.
     *
     * @param codePoint The typed code point.
     * @return The resolved {@link Result}; an unmapped code point yields a result with no key
     * code, no code point, and {@link SideEffect#NONE}.
     */
    public static Result map(int codePoint) {
        int resultingKeyCode = NONE;
        int resultingCodePoint = NONE;
        boolean altDown = false;
        SideEffect sideEffect = SideEffect.NONE;
        int lowerCase = Character.toLowerCase(codePoint);
        switch (lowerCase) {
            // Arrow keys.
            case 'w':
                resultingKeyCode = KeyEvent.KEYCODE_DPAD_UP;
                break;
            case 'a':
                resultingKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                break;
            case 's':
                resultingKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                break;
            case 'd':
                resultingKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                break;

            // Page up and down.
            case 'p':
                resultingKeyCode = KeyEvent.KEYCODE_PAGE_UP;
                break;
            case 'n':
                resultingKeyCode = KeyEvent.KEYCODE_PAGE_DOWN;
                break;

            // Some special keys:
            case 't':
                resultingKeyCode = KeyEvent.KEYCODE_TAB;
                break;
            case 'i':
                resultingKeyCode = KeyEvent.KEYCODE_INSERT;
                break;
            case 'h':
                resultingCodePoint = '~';
                break;

            // Special characters to input.
            case 'u':
                resultingCodePoint = '_';
                break;
            case 'l':
                resultingCodePoint = '|';
                break;

            // Function keys.
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                resultingKeyCode = (codePoint - '1') + KeyEvent.KEYCODE_F1;
                break;
            case '0':
                resultingKeyCode = KeyEvent.KEYCODE_F10;
                break;

            // Other special keys.
            case 'e':
                resultingCodePoint = /*Escape*/ 27;
                break;
            case '.':
                resultingCodePoint = /*^.*/ 28;
                break;

            case 'b': // alt+b, jumping backward in readline.
            case 'f': // alf+f, jumping forward in readline.
            case 'x': // alt+x, common in emacs.
                resultingCodePoint = lowerCase;
                altDown = true;
                break;

            // Volume control.
            case 'v':
                resultingCodePoint = NONE;
                sideEffect = SideEffect.ADJUST_VOLUME;
                break;

            // Writing mode:
            case 'q':
            case 'k':
                sideEffect = SideEffect.TOGGLE_TERMINAL_TOOLBAR;
                break;
        }

        return new Result(resultingKeyCode, resultingCodePoint, altDown, sideEffect);
    }
}
