package com.termux.app.terminal;

import android.view.KeyEvent;

import com.termux.app.terminal.VirtualFunctionKeyMapper.Result;
import com.termux.app.terminal.VirtualFunctionKeyMapper.SideEffect;

import org.junit.Assert;
import org.junit.Test;

public class VirtualFunctionKeyMapperTest {

    private static Result map(char c) {
        return VirtualFunctionKeyMapper.map(c);
    }

    @Test
    public void mapsArrowKeys() {
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_UP, map('w').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, map('a').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, map('s').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, map('d').keyCode);
    }

    @Test
    public void mapsArrowKeysCaseInsensitively() {
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_UP, map('W').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, map('D').keyCode);
    }

    @Test
    public void mapsPageKeys() {
        Assert.assertEquals(KeyEvent.KEYCODE_PAGE_UP, map('p').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_PAGE_DOWN, map('n').keyCode);
    }

    @Test
    public void mapsTabAndInsert() {
        Assert.assertEquals(KeyEvent.KEYCODE_TAB, map('t').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_INSERT, map('i').keyCode);
    }

    @Test
    public void mapsSpecialCharactersToCodePoints() {
        Assert.assertEquals('~', map('h').codePoint);
        Assert.assertEquals('_', map('u').codePoint);
        Assert.assertEquals('|', map('l').codePoint);
        Assert.assertEquals(VirtualFunctionKeyMapper.NONE, map('h').keyCode);
    }

    @Test
    public void mapsFunctionKeysOneThroughNine() {
        Assert.assertEquals(KeyEvent.KEYCODE_F1, map('1').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_F2, map('2').keyCode);
        Assert.assertEquals(KeyEvent.KEYCODE_F9, map('9').keyCode);
    }

    @Test
    public void mapsZeroToFunctionKeyTen() {
        Assert.assertEquals(KeyEvent.KEYCODE_F10, map('0').keyCode);
    }

    @Test
    public void mapsEscapeAndControlDot() {
        Assert.assertEquals(27, map('e').codePoint);
        Assert.assertEquals(28, map('.').codePoint);
    }

    @Test
    public void mapsReadlineKeysWithAltHeld() {
        for (char c : new char[]{'b', 'f', 'x'}) {
            Result result = map(c);
            Assert.assertEquals(c, result.codePoint);
            Assert.assertTrue("alt should be held for " + c, result.altDown);
            Assert.assertEquals(VirtualFunctionKeyMapper.NONE, result.keyCode);
        }
    }

    @Test
    public void mapsReadlineKeysCaseInsensitivelyToLowerCaseCodePoint() {
        Result result = map('X');
        Assert.assertEquals('x', result.codePoint);
        Assert.assertTrue(result.altDown);
    }

    @Test
    public void mapsVolumeKeyToAdjustVolumeSideEffectWithNoOutput() {
        Result result = map('v');
        Assert.assertEquals(SideEffect.ADJUST_VOLUME, result.sideEffect);
        Assert.assertEquals(VirtualFunctionKeyMapper.NONE, result.keyCode);
        Assert.assertEquals(VirtualFunctionKeyMapper.NONE, result.codePoint);
    }

    @Test
    public void mapsToolbarKeysToToggleSideEffect() {
        Assert.assertEquals(SideEffect.TOGGLE_TERMINAL_TOOLBAR, map('q').sideEffect);
        Assert.assertEquals(SideEffect.TOGGLE_TERMINAL_TOOLBAR, map('k').sideEffect);
    }

    @Test
    public void unmappedCodePointYieldsNoKeyCodeNoCodePointNoSideEffect() {
        Result result = map('z');
        Assert.assertEquals(VirtualFunctionKeyMapper.NONE, result.keyCode);
        Assert.assertEquals(VirtualFunctionKeyMapper.NONE, result.codePoint);
        Assert.assertFalse(result.altDown);
        Assert.assertEquals(SideEffect.NONE, result.sideEffect);
    }
}
