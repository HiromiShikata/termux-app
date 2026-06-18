package com.termux.shared.android;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.termux.shared.android.SettingsProviderUtils.SettingNamespace;
import com.termux.shared.android.SettingsProviderUtils.SettingType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class SettingsProviderUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void settingNamespaceDeclaresExactlyGlobalSecureAndSystem() {
        Assert.assertArrayEquals(
            new SettingNamespace[]{SettingNamespace.GLOBAL, SettingNamespace.SECURE, SettingNamespace.SYSTEM},
            SettingNamespace.values());
    }

    @Test
    public void settingTypeDeclaresAllSupportedTypesInOrder() {
        Assert.assertArrayEquals(
            new SettingType[]{SettingType.FLOAT, SettingType.INT, SettingType.LONG, SettingType.STRING, SettingType.URI},
            SettingType.values());
        Assert.assertEquals(SettingType.STRING, SettingType.valueOf("STRING"));
    }

    @Test
    public void getSettingsValueReturnsNullForMissingStringKeyBecauseGetStringDoesNotThrow() {
        Object value = SettingsProviderUtils.getSettingsValue(context(), SettingNamespace.GLOBAL,
            SettingType.STRING, "a_setting_that_does_not_exist", "fallback");
        Assert.assertNull(value);
    }

    @Test
    public void getSettingsValueReturnsDefaultForMissingIntKey() {
        Object value = SettingsProviderUtils.getSettingsValue(context(), SettingNamespace.SECURE,
            SettingType.INT, "an_int_setting_that_does_not_exist", -7);
        Assert.assertEquals(-7, value);
    }

    @Test
    public void getSettingsValueReturnsNullDefaultWhenNoFallbackProvided() {
        Object value = SettingsProviderUtils.getSettingsValue(context(), SettingNamespace.SYSTEM,
            SettingType.LONG, "a_long_setting_that_does_not_exist", null);
        Assert.assertNull(value);
    }

    @Test
    public void getSettingsValueReadsBackWrittenGlobalStringValue() {
        Settings.Global.putString(context().getContentResolver(), "termux_test_global_string", "stored");
        Object value = SettingsProviderUtils.getSettingsValue(context(), SettingNamespace.GLOBAL,
            SettingType.STRING, "termux_test_global_string", "fallback");
        Assert.assertEquals("stored", value);
    }

    @Test
    public void getSettingsValueReadsBackWrittenSecureIntValue() {
        Settings.Secure.putInt(context().getContentResolver(), "termux_test_secure_int", 42);
        Object value = SettingsProviderUtils.getSettingsValue(context(), SettingNamespace.SECURE,
            SettingType.INT, "termux_test_secure_int", 0);
        Assert.assertEquals(42, value);
    }

    @Test
    public void getSettingsValueForUriTypeReturnsContentUriForKey() {
        Object value = SettingsProviderUtils.getSettingsValue(context(), SettingNamespace.GLOBAL,
            SettingType.URI, "termux_test_uri_key", null);
        Assert.assertTrue(value instanceof Uri);
        Assert.assertTrue(value.toString().endsWith("termux_test_uri_key"));
    }
}
