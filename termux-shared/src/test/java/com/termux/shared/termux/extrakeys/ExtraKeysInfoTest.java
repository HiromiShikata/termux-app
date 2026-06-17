package com.termux.shared.termux.extrakeys;

import android.os.Build;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ExtraKeysInfoTest {

    private ExtraKeysInfo buildDefault(String propertiesInfo) throws JSONException {
        return new ExtraKeysInfo(propertiesInfo, "default", ExtraKeysConstants.CONTROL_CHARS_ALIASES);
    }

    @Test
    public void emptyMatrixProducesNoRows() throws JSONException {
        ExtraKeysInfo info = buildDefault("[]");
        Assert.assertEquals(0, info.getMatrix().length);
    }

    @Test
    public void singleRowOfStringKeysIsParsed() throws JSONException {
        ExtraKeysInfo info = buildDefault("[['ESC','TAB','CTRL']]");
        ExtraKeyButton[][] matrix = info.getMatrix();
        Assert.assertEquals(1, matrix.length);
        Assert.assertEquals(3, matrix[0].length);
        Assert.assertEquals("ESC", matrix[0][0].getKey());
        Assert.assertEquals("TAB", matrix[0][1].getKey());
        Assert.assertEquals("CTRL", matrix[0][2].getKey());
    }

    @Test
    public void multipleRowsAreParsed() throws JSONException {
        ExtraKeysInfo info = buildDefault("[['ESC','HOME'],['TAB','END','PGUP']]");
        ExtraKeyButton[][] matrix = info.getMatrix();
        Assert.assertEquals(2, matrix.length);
        Assert.assertEquals(2, matrix[0].length);
        Assert.assertEquals(3, matrix[1].length);
        Assert.assertEquals("PGUP", matrix[1][2].getKey());
    }

    @Test
    public void dictKeyWithPopupIsParsed() throws JSONException {
        ExtraKeysInfo info = buildDefault("[[{key: '-', popup: '|'}]]");
        ExtraKeyButton button = info.getMatrix()[0][0];
        Assert.assertEquals("-", button.getKey());
        Assert.assertNotNull(button.getPopup());
        Assert.assertEquals("|", button.getPopup().getKey());
    }

    @Test
    public void dictKeyWithMacroPopupIsParsed() throws JSONException {
        ExtraKeysInfo info = buildDefault("[[{key: 'ESC', popup: {macro: 'CTRL f d', display: 'tmux exit'}}]]");
        ExtraKeyButton button = info.getMatrix()[0][0];
        Assert.assertEquals("ESC", button.getKey());
        Assert.assertNotNull(button.getPopup());
        Assert.assertTrue(button.getPopup().isMacro());
        Assert.assertEquals("CTRL f d", button.getPopup().getKey());
        Assert.assertEquals("tmux exit", button.getPopup().getDisplay());
    }

    @Test
    public void aliasInKeyIsReplacedDuringMatrixBuild() throws JSONException {
        ExtraKeysInfo info = buildDefault("[['CONTROL']]");
        Assert.assertEquals("CTRL", info.getMatrix()[0][0].getKey());
    }

    @Test(expected = JSONException.class)
    public void nonStringNonObjectKeyThrows() throws JSONException {
        buildDefault("[[123]]");
    }

    @Test(expected = JSONException.class)
    public void invalidJsonThrows() throws JSONException {
        buildDefault("not-json");
    }

    @Test
    public void getCharDisplayMapForStyleArrowsOnly() {
        Assert.assertSame(ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.ARROWS_ONLY_CHAR_DISPLAY,
            ExtraKeysInfo.getCharDisplayMapForStyle("arrows-only"));
    }

    @Test
    public void getCharDisplayMapForStyleArrowsAll() {
        Assert.assertSame(ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.LOTS_OF_ARROWS_CHAR_DISPLAY,
            ExtraKeysInfo.getCharDisplayMapForStyle("arrows-all"));
    }

    @Test
    public void getCharDisplayMapForStyleAll() {
        Assert.assertSame(ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.FULL_ISO_CHAR_DISPLAY,
            ExtraKeysInfo.getCharDisplayMapForStyle("all"));
    }

    @Test
    public void getCharDisplayMapForStyleNoneReturnsEmptyMap() {
        ExtraKeysConstants.ExtraKeyDisplayMap map = ExtraKeysInfo.getCharDisplayMapForStyle("none");
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void getCharDisplayMapForStyleUnknownReturnsDefault() {
        Assert.assertSame(ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY,
            ExtraKeysInfo.getCharDisplayMapForStyle("unrecognized-style"));
    }

    @Test
    public void constructorWithExplicitDisplayMapAppliesIt() throws JSONException {
        ExtraKeysConstants.ExtraKeyDisplayMap displayMap = new ExtraKeysConstants.ExtraKeyDisplayMap();
        displayMap.put("LEFT", "<<");
        ExtraKeysInfo info = new ExtraKeysInfo("[['LEFT']]", displayMap, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        Assert.assertEquals("<<", info.getMatrix()[0][0].getDisplay());
    }
}
