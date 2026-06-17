package com.termux.shared.termux.extrakeys;

import android.os.Build;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class ExtraKeyButtonTest {

    private ExtraKeysConstants.ExtraKeyDisplayMap displayMap() {
        ExtraKeysConstants.ExtraKeyDisplayMap map = new ExtraKeysConstants.ExtraKeyDisplayMap();
        map.put("LEFT", "←");
        return map;
    }

    private ExtraKeysConstants.ExtraKeyDisplayMap aliasMap() {
        ExtraKeysConstants.ExtraKeyDisplayMap map = new ExtraKeysConstants.ExtraKeyDisplayMap();
        map.put("CONTROL", "CTRL");
        return map;
    }

    @Test
    public void keyConfigUsesKeyNameForKeyAndDisplay() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "LEFT");
        ExtraKeyButton button = new ExtraKeyButton(config, displayMap(), aliasMap());
        Assert.assertEquals("LEFT", button.getKey());
        Assert.assertEquals("←", button.getDisplay());
        Assert.assertFalse(button.isMacro());
        Assert.assertNull(button.getPopup());
    }

    @Test
    public void keyConfigWithoutDisplayMappingFallsBackToKeyName() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "HOME");
        ExtraKeyButton button = new ExtraKeyButton(config, displayMap(), aliasMap());
        Assert.assertEquals("HOME", button.getKey());
        Assert.assertEquals("HOME", button.getDisplay());
    }

    @Test
    public void explicitDisplayNameOverridesDisplayMap() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "LEFT");
        config.put(ExtraKeyButton.KEY_DISPLAY_NAME, "GoLeft");
        ExtraKeyButton button = new ExtraKeyButton(config, displayMap(), aliasMap());
        Assert.assertEquals("GoLeft", button.getDisplay());
    }

    @Test
    public void aliasIsReplacedInKey() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "CONTROL");
        ExtraKeyButton button = new ExtraKeyButton(config, displayMap(), aliasMap());
        Assert.assertEquals("CTRL", button.getKey());
    }

    @Test
    public void macroSplitsKeysOnSpaceAndJoinsDisplay() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_MACRO, "LEFT HOME");
        ExtraKeyButton button = new ExtraKeyButton(config, displayMap(), aliasMap());
        Assert.assertTrue(button.isMacro());
        Assert.assertEquals("LEFT HOME", button.getKey());
        Assert.assertEquals("← HOME", button.getDisplay());
    }

    @Test(expected = org.json.JSONException.class)
    public void bothKeyAndMacroThrows() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "LEFT");
        config.put(ExtraKeyButton.KEY_MACRO, "HOME END");
        new ExtraKeyButton(config, displayMap(), aliasMap());
    }

    @Test(expected = org.json.JSONException.class)
    public void neitherKeyNorMacroThrows() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_DISPLAY_NAME, "Orphan");
        new ExtraKeyButton(config, displayMap(), aliasMap());
    }

    @Test
    public void popupButtonIsRetained() throws Exception {
        JSONObject popupConfig = new JSONObject();
        popupConfig.put(ExtraKeyButton.KEY_KEY_NAME, "HOME");
        ExtraKeyButton popup = new ExtraKeyButton(popupConfig, displayMap(), aliasMap());

        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "LEFT");
        ExtraKeyButton button = new ExtraKeyButton(config, popup, displayMap(), aliasMap());

        Assert.assertNotNull(button.getPopup());
        Assert.assertEquals("HOME", button.getPopup().getKey());
    }

    @Test
    public void getStringFromJsonReturnsNullForMissingKey() throws Exception {
        JSONObject config = new JSONObject();
        config.put(ExtraKeyButton.KEY_KEY_NAME, "LEFT");
        ExtraKeyButton button = new ExtraKeyButton(config, displayMap(), aliasMap());
        Assert.assertNull(button.getStringFromJson(config, "nonexistent"));
        Assert.assertEquals("LEFT", button.getStringFromJson(config, ExtraKeyButton.KEY_KEY_NAME));
    }

    @Test
    public void replaceAliasReturnsKeyWhenNotAliased() {
        Assert.assertEquals("PLAIN", ExtraKeyButton.replaceAlias(aliasMap(), "PLAIN"));
        Assert.assertEquals("CTRL", ExtraKeyButton.replaceAlias(aliasMap(), "CONTROL"));
    }
}
