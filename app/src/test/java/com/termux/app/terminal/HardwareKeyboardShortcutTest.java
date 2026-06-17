package com.termux.app.terminal;

import android.view.KeyEvent;

import com.termux.app.terminal.HardwareKeyboardShortcut.Action;
import com.termux.app.terminal.HardwareKeyboardShortcut.Result;

import org.junit.Assert;
import org.junit.Test;

public class HardwareKeyboardShortcutTest {

    private static final int NO_KEY = KeyEvent.KEYCODE_UNKNOWN;

    private static Result decideChar(char c) {
        return HardwareKeyboardShortcut.decide(NO_KEY, c, 0);
    }

    @Test
    public void nextSessionFromDpadDownOrLetterN() {
        Assert.assertEquals(Action.SWITCH_TO_NEXT_SESSION,
            HardwareKeyboardShortcut.decide(KeyEvent.KEYCODE_DPAD_DOWN, 0, 0).action);
        Assert.assertEquals(Action.SWITCH_TO_NEXT_SESSION, decideChar('n').action);
    }

    @Test
    public void previousSessionFromDpadUpOrLetterP() {
        Assert.assertEquals(Action.SWITCH_TO_PREVIOUS_SESSION,
            HardwareKeyboardShortcut.decide(KeyEvent.KEYCODE_DPAD_UP, 0, 0).action);
        Assert.assertEquals(Action.SWITCH_TO_PREVIOUS_SESSION, decideChar('p').action);
    }

    @Test
    public void drawerOpenAndClose() {
        Assert.assertEquals(Action.OPEN_DRAWER,
            HardwareKeyboardShortcut.decide(KeyEvent.KEYCODE_DPAD_RIGHT, 0, 0).action);
        Assert.assertEquals(Action.CLOSE_DRAWER,
            HardwareKeyboardShortcut.decide(KeyEvent.KEYCODE_DPAD_LEFT, 0, 0).action);
    }

    @Test
    public void letterShortcuts() {
        Assert.assertEquals(Action.TOGGLE_SOFT_KEYBOARD, decideChar('k').action);
        Assert.assertEquals(Action.SHOW_CONTEXT_MENU, decideChar('m').action);
        Assert.assertEquals(Action.RENAME_SESSION, decideChar('r').action);
        Assert.assertEquals(Action.CREATE_SESSION, decideChar('c').action);
        Assert.assertEquals(Action.SHOW_URL_SELECTION, decideChar('u').action);
        Assert.assertEquals(Action.PASTE, decideChar('v').action);
    }

    @Test
    public void increaseFontSizeFromPlus() {
        Assert.assertEquals(Action.INCREASE_FONT_SIZE, decideChar('+').action);
    }

    @Test
    public void increaseFontSizeFromShiftedPlusWhenUnshiftedCharDoesNotMatch() {
        Result result = HardwareKeyboardShortcut.decide(NO_KEY, '=', '+');
        Assert.assertEquals(Action.INCREASE_FONT_SIZE, result.action);
    }

    @Test
    public void decreaseFontSizeFromMinus() {
        Assert.assertEquals(Action.DECREASE_FONT_SIZE, decideChar('-').action);
    }

    @Test
    public void switchToSessionIndexFromDigitsOneThroughNine() {
        Result one = decideChar('1');
        Assert.assertEquals(Action.SWITCH_TO_SESSION_INDEX, one.action);
        Assert.assertEquals(0, one.sessionIndex);

        Result nine = decideChar('9');
        Assert.assertEquals(Action.SWITCH_TO_SESSION_INDEX, nine.action);
        Assert.assertEquals(8, nine.sessionIndex);
    }

    @Test
    public void digitZeroIsNotASessionIndexShortcut() {
        Assert.assertEquals(Action.NONE, decideChar('0').action);
    }

    @Test
    public void unmatchedKeyYieldsNoneWithNoSessionIndex() {
        Result result = decideChar('z');
        Assert.assertEquals(Action.NONE, result.action);
        Assert.assertEquals(-1, result.sessionIndex);
    }

    @Test
    public void nonIndexActionsCarryNegativeSessionIndex() {
        Assert.assertEquals(-1, decideChar('v').sessionIndex);
    }
}
