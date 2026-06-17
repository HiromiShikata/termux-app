package com.termux.shared.termux.extrakeys;

import android.os.Build;
import android.view.KeyEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ExtraKeysConstantsTest {

    @Test
    public void cleverMapReturnsDefaultWhenKeyMissing() {
        ExtraKeysConstants.ExtraKeyDisplayMap map = new ExtraKeysConstants.ExtraKeyDisplayMap();
        Assert.assertEquals("fallback", map.get("MISSING", "fallback"));
    }

    @Test
    public void cleverMapReturnsStoredValueWhenKeyPresent() {
        ExtraKeysConstants.ExtraKeyDisplayMap map = new ExtraKeysConstants.ExtraKeyDisplayMap();
        map.put("LEFT", "←");
        Assert.assertEquals("←", map.get("LEFT", "fallback"));
    }

    @Test
    public void primaryKeyCodesMapKnownNamesToKeyEventCodes() {
        Assert.assertEquals(Integer.valueOf(KeyEvent.KEYCODE_ENTER),
            ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS.get("ENTER"));
        Assert.assertEquals(Integer.valueOf(KeyEvent.KEYCODE_DPAD_LEFT),
            ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS.get("LEFT"));
        Assert.assertEquals(Integer.valueOf(KeyEvent.KEYCODE_F12),
            ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS.get("F12"));
    }

    @Test
    public void repetitiveKeysContainNavigationKeys() {
        Assert.assertTrue(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS.contains("UP"));
        Assert.assertTrue(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS.contains("PGDN"));
        Assert.assertFalse(ExtraKeysConstants.PRIMARY_REPETITIVE_KEYS.contains("ENTER"));
    }

    @Test
    public void classicArrowsDisplayMapsArrowNames() {
        Assert.assertEquals("←", ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.CLASSIC_ARROWS_DISPLAY.get("LEFT"));
        Assert.assertEquals("→", ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.CLASSIC_ARROWS_DISPLAY.get("RIGHT"));
        Assert.assertEquals("↑", ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.CLASSIC_ARROWS_DISPLAY.get("UP"));
        Assert.assertEquals("↓", ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.CLASSIC_ARROWS_DISPLAY.get("DOWN"));
    }

    @Test
    public void fullIsoDisplayAggregatesMultipleMaps() {
        ExtraKeysConstants.ExtraKeyDisplayMap fullIso = ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.FULL_ISO_CHAR_DISPLAY;
        Assert.assertEquals("←", fullIso.get("LEFT"));
        Assert.assertEquals("↲", fullIso.get("ENTER"));
        Assert.assertEquals("⇱", fullIso.get("HOME"));
        Assert.assertEquals("⎈", fullIso.get("CTRL"));
        Assert.assertEquals("―", fullIso.get("-"));
    }

    @Test
    public void arrowsOnlyDisplayExcludesWellKnownCharacters() {
        ExtraKeysConstants.ExtraKeyDisplayMap arrowsOnly = ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.ARROWS_ONLY_CHAR_DISPLAY;
        Assert.assertEquals("←", arrowsOnly.get("LEFT"));
        Assert.assertNull(arrowsOnly.get("ENTER"));
    }

    @Test
    public void defaultDisplayContainsArrowsAndWellKnownButNotLessKnown() {
        ExtraKeysConstants.ExtraKeyDisplayMap defaultMap = ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY;
        Assert.assertEquals("→", defaultMap.get("RIGHT"));
        Assert.assertEquals("↹", defaultMap.get("TAB"));
        Assert.assertNull(defaultMap.get("HOME"));
    }

    @Test
    public void controlCharsAliasesMapAlternateSpellings() {
        Assert.assertEquals("ESC", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("ESCAPE"));
        Assert.assertEquals("CTRL", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("CONTROL"));
        Assert.assertEquals("PGUP", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("PAGE UP"));
        Assert.assertEquals("PGDN", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("PAGE-DOWN"));
        Assert.assertEquals("BKSP", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("BACKSPACE"));
        Assert.assertEquals("\\", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("BACKSLASH"));
    }

    @Test
    public void controlCharsAliasesReturnDefaultForUnmappedKey() {
        Assert.assertEquals("UNMAPPED", ExtraKeysConstants.CONTROL_CHARS_ALIASES.get("UNMAPPED", "UNMAPPED"));
    }
}
