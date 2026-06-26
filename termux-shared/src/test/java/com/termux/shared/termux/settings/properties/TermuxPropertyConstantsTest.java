package com.termux.shared.termux.settings.properties;

import com.google.common.collect.ImmutableBiMap;
import com.termux.shared.termux.TermuxConstants;

import org.junit.Assert;
import org.junit.Test;

public class TermuxPropertyConstantsTest {

    @Test
    public void bellBehaviourMapContainsAllValues() {
        ImmutableBiMap<String, Integer> map = TermuxPropertyConstants.MAP_BELL_BEHAVIOUR;
        Assert.assertEquals(3, map.size());
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_VIBRATE),
            map.get(TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_VIBRATE));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_BEEP),
            map.get(TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_BEEP));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_IGNORE),
            map.get(TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_IGNORE));
    }

    @Test
    public void bellBehaviourMapInverseResolvesNames() {
        ImmutableBiMap<Integer, String> inverse = TermuxPropertyConstants.MAP_BELL_BEHAVIOUR.inverse();
        Assert.assertEquals(TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_VIBRATE,
            inverse.get(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_VIBRATE));
        Assert.assertEquals(TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_BEEP,
            inverse.get(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_BEEP));
    }

    @Test
    public void defaultBellBehaviourMatchesVibrate() {
        Assert.assertEquals(TermuxPropertyConstants.VALUE_BELL_BEHAVIOUR_VIBRATE,
            TermuxPropertyConstants.DEFAULT_VALUE_BELL_BEHAVIOUR);
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_VIBRATE,
            TermuxPropertyConstants.DEFAULT_IVALUE_BELL_BEHAVIOUR);
    }

    @Test
    public void terminalCursorStyleMapContainsAllValues() {
        ImmutableBiMap<String, Integer> map = TermuxPropertyConstants.MAP_TERMINAL_CURSOR_STYLE;
        Assert.assertEquals(3, map.size());
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_BLOCK),
            map.get(TermuxPropertyConstants.VALUE_TERMINAL_CURSOR_STYLE_BLOCK));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_UNDERLINE),
            map.get(TermuxPropertyConstants.VALUE_TERMINAL_CURSOR_STYLE_UNDERLINE));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.IVALUE_TERMINAL_CURSOR_STYLE_BAR),
            map.get(TermuxPropertyConstants.VALUE_TERMINAL_CURSOR_STYLE_BAR));
    }

    @Test
    public void sessionShortcutsMapMapsKeysToActions() {
        ImmutableBiMap<String, Integer> map = TermuxPropertyConstants.MAP_SESSION_SHORTCUTS;
        Assert.assertEquals(4, map.size());
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.ACTION_SHORTCUT_CREATE_SESSION),
            map.get(TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.ACTION_SHORTCUT_NEXT_SESSION),
            map.get(TermuxPropertyConstants.KEY_SHORTCUT_NEXT_SESSION));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.ACTION_SHORTCUT_PREVIOUS_SESSION),
            map.get(TermuxPropertyConstants.KEY_SHORTCUT_PREVIOUS_SESSION));
        Assert.assertEquals(Integer.valueOf(TermuxPropertyConstants.ACTION_SHORTCUT_RENAME_SESSION),
            map.get(TermuxPropertyConstants.KEY_SHORTCUT_RENAME_SESSION));
    }

    @Test
    public void backKeyBehaviourMapIsSelfMapping() {
        ImmutableBiMap<String, String> map = TermuxPropertyConstants.MAP_BACK_KEY_BEHAVIOUR;
        Assert.assertEquals(2, map.size());
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_BACK,
            map.get(TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_BACK));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE,
            map.get(TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_BACK,
            TermuxPropertyConstants.DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR);
    }

    @Test
    public void nightModeMapContainsAllModes() {
        ImmutableBiMap<String, String> map = TermuxPropertyConstants.MAP_NIGHT_MODE;
        Assert.assertEquals(3, map.size());
        Assert.assertTrue(map.containsKey(TermuxPropertyConstants.IVALUE_NIGHT_MODE_TRUE));
        Assert.assertTrue(map.containsKey(TermuxPropertyConstants.IVALUE_NIGHT_MODE_FALSE));
        Assert.assertTrue(map.containsKey(TermuxPropertyConstants.IVALUE_NIGHT_MODE_SYSTEM));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_NIGHT_MODE_SYSTEM,
            TermuxPropertyConstants.DEFAULT_IVALUE_NIGHT_MODE);
    }

    @Test
    public void softKeyboardToggleBehaviourMapIsSelfMapping() {
        ImmutableBiMap<String, String> map = TermuxPropertyConstants.MAP_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR;
        Assert.assertEquals(2, map.size());
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE,
            map.get(TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE,
            map.get(TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE,
            TermuxPropertyConstants.DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR);
    }

    @Test
    public void volumeKeysBehaviourMapIsSelfMapping() {
        ImmutableBiMap<String, String> map = TermuxPropertyConstants.MAP_VOLUME_KEYS_BEHAVIOUR;
        Assert.assertEquals(2, map.size());
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL,
            map.get(TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME,
            map.get(TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME));
        Assert.assertEquals(TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL,
            TermuxPropertyConstants.DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR);
    }

    @Test
    public void appPropertiesListContainsAllRegisteredKeys() {
        Assert.assertEquals(32, TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST.size());
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_BELL_BEHAVIOUR));
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_NIGHT_MODE));
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST
            .contains(TermuxConstants.PROP_ALLOW_EXTERNAL_APPS));
    }

    @Test
    public void defaultFalseBooleanListContainsExpectedKeys() {
        Assert.assertEquals(11, TermuxPropertyConstants.TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.size());
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_DISABLE_FILE_SHARE_RECEIVER));
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_USE_FULLSCREEN));
        Assert.assertFalse(TermuxPropertyConstants.TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_EXTRA_KEYS_TEXT_ALL_CAPS));
    }

    @Test
    public void defaultTrueBooleanListContainsExpectedKeys() {
        Assert.assertEquals(2, TermuxPropertyConstants.TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.size());
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_EXTRA_KEYS_TEXT_ALL_CAPS));
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST
            .contains(TermuxPropertyConstants.KEY_RUN_TERMUX_AM_SOCKET_SERVER));
    }

    @Test
    public void invertedBooleanListsAreEmpty() {
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_INVERETED_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.isEmpty());
        Assert.assertTrue(TermuxPropertyConstants.TERMUX_DEFAULT_INVERETED_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.isEmpty());
    }

    @Test
    public void deleteTmpdirFilesConstantsHaveExpectedRange() {
        Assert.assertEquals(-1, TermuxPropertyConstants.IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT_MIN);
        Assert.assertEquals(100000, TermuxPropertyConstants.IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT_MAX);
        Assert.assertEquals(3, TermuxPropertyConstants.DEFAULT_IVALUE_DELETE_TMPDIR_FILES_OLDER_THAN_X_DAYS_ON_EXIT);
    }

    @Test
    public void terminalMarginConstantsHaveExpectedRanges() {
        Assert.assertEquals(0, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_HORIZONTAL_MIN);
        Assert.assertEquals(100, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_HORIZONTAL_MAX);
        Assert.assertEquals(0, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_MARGIN_HORIZONTAL);
        Assert.assertEquals(0, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_VERTICAL_MIN);
        Assert.assertEquals(100, TermuxPropertyConstants.IVALUE_TERMINAL_MARGIN_VERTICAL_MAX);
        Assert.assertEquals(0, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_MARGIN_VERTICAL);
    }

    @Test
    public void toolbarHeightScaleFactorConstantsHaveExpectedRange() {
        Assert.assertEquals(0.4f, TermuxPropertyConstants.IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MIN, 0.0001f);
        Assert.assertEquals(3f, TermuxPropertyConstants.IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR_MAX, 0.0001f);
        Assert.assertEquals(1f, TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR, 0.0001f);
    }

    @Test
    public void defaultWorkingDirectoryMatchesTermuxHome() {
        Assert.assertEquals(TermuxConstants.TERMUX_HOME_DIR_PATH,
            TermuxPropertyConstants.DEFAULT_IVALUE_DEFAULT_WORKING_DIRECTORY);
    }

    @Test
    public void defaultExtraKeysStyleIsDefault() {
        Assert.assertEquals("default", TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE);
    }
}
