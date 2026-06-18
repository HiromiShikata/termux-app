package com.termux.shared.termux.settings.properties;

import com.termux.shared.termux.TermuxConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@RunWith(RobolectricTestRunner.class)
public class TermuxSharedPropertiesValueTest {

    @Test
    public void getCodePointForSessionShortcutsReturnsNullForNullKey() {
        Assert.assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts(null, "ctrl + a"));
    }

    @Test
    public void getCodePointForSessionShortcutsReturnsNullForNullValue() {
        Assert.assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts(
            TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, null));
    }

    @Test
    public void getCodePointForSessionShortcutsParsesSingleCharacterShortcut() {
        Integer codePoint = TermuxSharedProperties.getCodePointForSessionShortcuts(
            TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, "ctrl + t");

        Assert.assertNotNull(codePoint);
        Assert.assertEquals('t', (int) codePoint.intValue());
    }

    @Test
    public void getCodePointForSessionShortcutsIsCaseInsensitiveAndTrims() {
        Integer codePoint = TermuxSharedProperties.getCodePointForSessionShortcuts(
            TermuxPropertyConstants.KEY_SHORTCUT_NEXT_SESSION, "  CTRL + N  ");

        Assert.assertNotNull(codePoint);
        Assert.assertEquals('n', (int) codePoint.intValue());
    }

    @Test
    public void getCodePointForSessionShortcutsReturnsNullWhenModifierIsNotCtrl() {
        Assert.assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts(
            TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, "alt + a"));
    }

    @Test
    public void getCodePointForSessionShortcutsReturnsNullWhenNoPlusSeparator() {
        Assert.assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts(
            TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, "ctrl"));
    }

    @Test
    public void getCodePointForSessionShortcutsReturnsNullWhenInputTooLong() {
        Assert.assertNull(TermuxSharedProperties.getCodePointForSessionShortcuts(
            TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, "ctrl + abc"));
    }

    @Test
    public void getDefaultWorkingDirectoryReturnsDefaultForNullPath() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY,
            TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(null));
    }

    @Test
    public void getDefaultWorkingDirectoryReturnsDefaultForEmptyPath() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY,
            TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(""));
    }

    @Test
    public void getDefaultWorkingDirectoryReturnsDefaultForNonExistentPath() {
        String nonExistent = new File(System.getProperty("java.io.tmpdir"),
            "tsp_missing_" + System.nanoTime()).getAbsolutePath();

        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY,
            TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(nonExistent));
    }

    @Test
    public void getDefaultWorkingDirectoryReturnsDefaultWhenPathIsRegularFile() throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir"), "tsp_file_" + System.nanoTime());
        Assert.assertTrue(file.createNewFile());
        file.deleteOnExit();

        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY,
            TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(file.getAbsolutePath()));
    }

    @Test
    public void getDefaultWorkingDirectoryReturnsPathWhenReadableDirectoryExists() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "tsp_dir_" + System.nanoTime());
        Assert.assertTrue(dir.mkdirs());
        dir.deleteOnExit();

        Assert.assertEquals(dir.getAbsolutePath(),
            TermuxSharedProperties.getDefaultWorkingDirectoryInternalPropertyValueFromValue(dir.getAbsolutePath()));
    }

    @Test
    public void getBellBehaviourMapsKnownValue() {
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_BEEP,
            TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue(
                TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_BEEP));
    }

    @Test
    public void getBellBehaviourReturnsDefaultForUnknownValue() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_BELL_BEHAVIOUR,
            TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue("not-a-bell-mode"));
    }

    @Test
    public void getBellBehaviourReturnsDefaultForNullValue() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_BELL_BEHAVIOUR,
            TermuxSharedProperties.getBellBehaviourInternalPropertyValueFromValue(null));
    }

    @Test
    public void getTerminalCursorBlinkRateKeepsInRangeValue() {
        int inRange = TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_BLINK_RATE_MIN + 1;

        Assert.assertEquals(inRange,
            TermuxSharedProperties.getTerminalCursorBlinkRateInternalPropertyValueFromValue(String.valueOf(inRange)));
    }

    @Test
    public void getTerminalCursorBlinkRateReturnsDefaultForValueBelowMinimum() {
        int belowMin = TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_BLINK_RATE_MIN - 1;

        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE,
            TermuxSharedProperties.getTerminalCursorBlinkRateInternalPropertyValueFromValue(String.valueOf(belowMin)));
    }

    @Test
    public void getTerminalCursorBlinkRateReturnsDefaultForValueAboveMaximum() {
        int aboveMax = TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_BLINK_RATE_MAX + 1;

        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE,
            TermuxSharedProperties.getTerminalCursorBlinkRateInternalPropertyValueFromValue(String.valueOf(aboveMax)));
    }

    @Test
    public void getTerminalCursorBlinkRateReturnsDefaultForNonNumericValue() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE,
            TermuxSharedProperties.getTerminalCursorBlinkRateInternalPropertyValueFromValue("not-a-number"));
    }

    @Test
    public void getTerminalTranscriptRowsKeepsInRangeValue() {
        int inRange = TermuxPropertyConstants.IVALUE_TERMINAL_TRANSCRIPT_ROWS_MIN + 5;

        Assert.assertEquals(inRange,
            TermuxSharedProperties.getTerminalTranscriptRowsInternalPropertyValueFromValue(String.valueOf(inRange)));
    }

    @Test
    public void getTerminalTranscriptRowsReturnsDefaultForValueBelowMinimum() {
        int belowMin = TermuxPropertyConstants.IVALUE_TERMINAL_TRANSCRIPT_ROWS_MIN - 1;

        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TRANSCRIPT_ROWS,
            TermuxSharedProperties.getTerminalTranscriptRowsInternalPropertyValueFromValue(String.valueOf(belowMin)));
    }

    @Test
    public void getNightModeMapsKnownValue() {
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_NIGHT_MODE_TRUE,
            TermuxSharedProperties.getNightModeInternalPropertyValueFromValue(
                TermuxPropertyConstants.IVALUE_NIGHT_MODE_TRUE));
    }

    @Test
    public void getNightModeReturnsDefaultForUnknownValue() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_NIGHT_MODE,
            TermuxSharedProperties.getNightModeInternalPropertyValueFromValue("not-a-mode"));
    }

    @Test
    public void getExtraKeysReturnsValueWhenSet() {
        Assert.assertEquals("[['a','b']]",
            TermuxSharedProperties.getExtraKeysInternalPropertyValueFromValue("[['a','b']]"));
    }

    @Test
    public void getExtraKeysReturnsDefaultForNull() {
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS,
            TermuxSharedProperties.getExtraKeysInternalPropertyValueFromValue(null));
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueReturnsNullForNullKey() {
        Assert.assertNull(TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null, null, "value"));
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDispatchesBellBehaviour() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxPropertyConstants.KEY_BELL_BEHAVIOUR, TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_IGNORE);

        Assert.assertEquals(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_IGNORE, result);
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDispatchesCursorBlinkRate() {
        int inRange = TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_BLINK_RATE_MIN + 2;

        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxPropertyConstants.KEY_TERMINAL_CURSOR_BLINK_RATE, String.valueOf(inRange));

        Assert.assertEquals(inRange, result);
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDispatchesToolbarHeightScaleFactorAsFloat() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxPropertyConstants.KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR, "not-a-float");

        Assert.assertTrue(result instanceof Float);
        Assert.assertEquals(TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR,
            (Float) result, 0.0001f);
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDispatchesSessionShortcut() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION, "ctrl + c");

        Assert.assertTrue(result instanceof Integer);
        Assert.assertEquals('c', ((Integer) result).intValue());
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDispatchesNightMode() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxPropertyConstants.KEY_NIGHT_MODE, TermuxPropertyConstants.IVALUE_NIGHT_MODE_FALSE);

        Assert.assertEquals(TermuxPropertyConstants.IVALUE_NIGHT_MODE_FALSE, result);
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDispatchesDefaultFalseBooleanProperty() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxConstants.PROP_ALLOW_EXTERNAL_APPS, "true");

        Assert.assertEquals(true, result);
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueDefaultsUnknownBooleanPropertyToFalse() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            TermuxConstants.PROP_ALLOW_EXTERNAL_APPS, null);

        Assert.assertEquals(false, result);
    }

    @Test
    public void getInternalTermuxPropertyValueFromValueReturnsRawValueForUnrecognizedKey() {
        Object result = TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(null,
            "a-completely-unknown-key", "raw-value");

        Assert.assertEquals("raw-value", result);
    }

    @Test
    public void replaceUseBlackUIReturnsPropertiesUnchangedWhenKeyAbsent() {
        Properties properties = new Properties();
        properties.setProperty("some-other-key", "value");

        Properties result = TermuxSharedProperties.replaceUseBlackUIProperty(properties);

        Assert.assertSame(properties, result);
        Assert.assertEquals("value", result.getProperty("some-other-key"));
        Assert.assertNull(result.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE));
    }

    @Test
    public void replaceUseBlackUITrueMapsToNightModeTrueAndRemovesDeprecatedKey() {
        Properties properties = new Properties();
        properties.setProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI, "true");

        Properties result = TermuxSharedProperties.replaceUseBlackUIProperty(properties);

        Assert.assertNull(result.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_NIGHT_MODE_TRUE,
            result.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE));
    }

    @Test
    public void replaceUseBlackUIFalseMapsToNightModeFalse() {
        Properties properties = new Properties();
        properties.setProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI, "false");

        Properties result = TermuxSharedProperties.replaceUseBlackUIProperty(properties);

        Assert.assertNull(result.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_NIGHT_MODE_FALSE,
            result.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE));
    }

    @Test
    public void replaceUseBlackUIDoesNotOverrideExplicitNightMode() {
        Properties properties = new Properties();
        properties.setProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI, "true");
        properties.setProperty(TermuxPropertyConstants.KEY_NIGHT_MODE,
            TermuxPropertyConstants.IVALUE_NIGHT_MODE_SYSTEM);

        Properties result = TermuxSharedProperties.replaceUseBlackUIProperty(properties);

        Assert.assertNull(result.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_NIGHT_MODE_SYSTEM,
            result.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE));
    }

    @Test
    public void replaceUseBlackUIWithInvalidBooleanRemovesKeyWithoutSettingNightMode() {
        Properties properties = new Properties();
        properties.setProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI, "neither-true-nor-false");

        Properties result = TermuxSharedProperties.replaceUseBlackUIProperty(properties);

        Assert.assertNull(result.getProperty(TermuxPropertyConstants.KEY_USE_BLACK_UI));
        Assert.assertNull(result.getProperty(TermuxPropertyConstants.KEY_NIGHT_MODE));
    }
}
