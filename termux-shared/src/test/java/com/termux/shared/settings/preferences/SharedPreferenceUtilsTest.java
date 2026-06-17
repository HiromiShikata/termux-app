package com.termux.shared.settings.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SharedPreferenceUtilsTest {

    private SharedPreferences preferences;

    @Before
    public void setUp() {
        preferences = RuntimeEnvironment.getApplication()
            .getSharedPreferences("shared_preference_utils_test", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @Test
    public void getPrivateSharedPreferencesReturnsUsableInstance() {
        SharedPreferences result = SharedPreferenceUtils.getPrivateSharedPreferences(
            RuntimeEnvironment.getApplication(), "private_prefs");
        Assert.assertNotNull(result);
        SharedPreferenceUtils.setBoolean(result, "flag", true, true);
        Assert.assertTrue(SharedPreferenceUtils.getBoolean(result, "flag", false));
    }

    @Test
    public void getPrivateAndMultiProcessSharedPreferencesReturnsUsableInstance() {
        SharedPreferences result = SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(
            RuntimeEnvironment.getApplication(), "multi_process_prefs");
        Assert.assertNotNull(result);
    }

    @Test
    public void getBooleanReturnsDefaultForNullPreferences() {
        Assert.assertTrue(SharedPreferenceUtils.getBoolean(null, "key", true));
        Assert.assertFalse(SharedPreferenceUtils.getBoolean(null, "key", false));
    }

    @Test
    public void setAndGetBooleanRoundTripsWithApply() {
        SharedPreferenceUtils.setBoolean(preferences, "boolean_key", true, false);
        Assert.assertTrue(SharedPreferenceUtils.getBoolean(preferences, "boolean_key", false));
    }

    @Test
    public void setAndGetBooleanRoundTripsWithCommit() {
        SharedPreferenceUtils.setBoolean(preferences, "boolean_key", true, true);
        Assert.assertTrue(SharedPreferenceUtils.getBoolean(preferences, "boolean_key", false));
    }

    @Test
    public void setBooleanIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setBoolean(null, "boolean_key", true, false);
        Assert.assertFalse(SharedPreferenceUtils.getBoolean(preferences, "boolean_key", false));
    }

    @Test
    public void getBooleanReturnsDefaultOnClassCastException() {
        preferences.edit().putString("boolean_key", "not_a_boolean").commit();
        Assert.assertTrue(SharedPreferenceUtils.getBoolean(preferences, "boolean_key", true));
    }

    @Test
    public void getFloatReturnsDefaultForNullPreferences() {
        Assert.assertEquals(1.5f, SharedPreferenceUtils.getFloat(null, "key", 1.5f), 0.0001f);
    }

    @Test
    public void setAndGetFloatRoundTripsWithApplyAndCommit() {
        SharedPreferenceUtils.setFloat(preferences, "float_key", 3.25f, false);
        Assert.assertEquals(3.25f, SharedPreferenceUtils.getFloat(preferences, "float_key", 0f), 0.0001f);
        SharedPreferenceUtils.setFloat(preferences, "float_key", 9.5f, true);
        Assert.assertEquals(9.5f, SharedPreferenceUtils.getFloat(preferences, "float_key", 0f), 0.0001f);
    }

    @Test
    public void setFloatIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setFloat(null, "float_key", 2f, false);
        Assert.assertEquals(0f, SharedPreferenceUtils.getFloat(preferences, "float_key", 0f), 0.0001f);
    }

    @Test
    public void getFloatReturnsDefaultOnClassCastException() {
        preferences.edit().putString("float_key", "not_a_float").commit();
        Assert.assertEquals(7f, SharedPreferenceUtils.getFloat(preferences, "float_key", 7f), 0.0001f);
    }

    @Test
    public void getIntReturnsDefaultForNullPreferences() {
        Assert.assertEquals(42, SharedPreferenceUtils.getInt(null, "key", 42));
    }

    @Test
    public void setAndGetIntRoundTripsWithApplyAndCommit() {
        SharedPreferenceUtils.setInt(preferences, "int_key", 11, false);
        Assert.assertEquals(11, SharedPreferenceUtils.getInt(preferences, "int_key", 0));
        SharedPreferenceUtils.setInt(preferences, "int_key", 22, true);
        Assert.assertEquals(22, SharedPreferenceUtils.getInt(preferences, "int_key", 0));
    }

    @Test
    public void setIntIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setInt(null, "int_key", 5, false);
        Assert.assertEquals(0, SharedPreferenceUtils.getInt(preferences, "int_key", 0));
    }

    @Test
    public void getIntReturnsDefaultOnClassCastException() {
        preferences.edit().putString("int_key", "not_an_int").commit();
        Assert.assertEquals(3, SharedPreferenceUtils.getInt(preferences, "int_key", 3));
    }

    @Test
    public void getAndIncrementIntReturnsDefaultForNullPreferences() {
        Assert.assertEquals(8, SharedPreferenceUtils.getAndIncrementInt(null, "key", 8, false, null));
    }

    @Test
    public void getAndIncrementIntReturnsPreviousValueAndStoresIncrement() {
        SharedPreferenceUtils.setInt(preferences, "counter", 4, true);
        int previous = SharedPreferenceUtils.getAndIncrementInt(preferences, "counter", 0, true, null);
        Assert.assertEquals(4, previous);
        Assert.assertEquals(5, SharedPreferenceUtils.getInt(preferences, "counter", 0));
    }

    @Test
    public void getAndIncrementIntResetsNegativeCurrentValue() {
        SharedPreferenceUtils.setInt(preferences, "counter", -3, true);
        int previous = SharedPreferenceUtils.getAndIncrementInt(preferences, "counter", -3, true, 0);
        Assert.assertEquals(0, previous);
        Assert.assertEquals(1, SharedPreferenceUtils.getInt(preferences, "counter", -1));
    }

    @Test
    public void getAndIncrementIntResetsOnOverflow() {
        SharedPreferenceUtils.setInt(preferences, "counter", Integer.MAX_VALUE, true);
        int previous = SharedPreferenceUtils.getAndIncrementInt(preferences, "counter", 0, true, 0);
        Assert.assertEquals(Integer.MAX_VALUE, previous);
        Assert.assertEquals(0, SharedPreferenceUtils.getInt(preferences, "counter", -1));
    }

    @Test
    public void getLongReturnsDefaultForNullPreferences() {
        Assert.assertEquals(99L, SharedPreferenceUtils.getLong(null, "key", 99L));
    }

    @Test
    public void setAndGetLongRoundTripsWithApplyAndCommit() {
        SharedPreferenceUtils.setLong(preferences, "long_key", 123L, false);
        Assert.assertEquals(123L, SharedPreferenceUtils.getLong(preferences, "long_key", 0L));
        SharedPreferenceUtils.setLong(preferences, "long_key", 456L, true);
        Assert.assertEquals(456L, SharedPreferenceUtils.getLong(preferences, "long_key", 0L));
    }

    @Test
    public void setLongIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setLong(null, "long_key", 7L, false);
        Assert.assertEquals(0L, SharedPreferenceUtils.getLong(preferences, "long_key", 0L));
    }

    @Test
    public void getLongReturnsDefaultOnClassCastException() {
        preferences.edit().putString("long_key", "not_a_long").commit();
        Assert.assertEquals(15L, SharedPreferenceUtils.getLong(preferences, "long_key", 15L));
    }

    @Test
    public void getStringReturnsDefaultForNullPreferences() {
        Assert.assertEquals("fallback",
            SharedPreferenceUtils.getString(null, "key", "fallback", true));
    }

    @Test
    public void setAndGetStringRoundTripsWithApplyAndCommit() {
        SharedPreferenceUtils.setString(preferences, "string_key", "hello", false);
        Assert.assertEquals("hello",
            SharedPreferenceUtils.getString(preferences, "string_key", "def", true));
        SharedPreferenceUtils.setString(preferences, "string_key", "world", true);
        Assert.assertEquals("world",
            SharedPreferenceUtils.getString(preferences, "string_key", "def", true));
    }

    @Test
    public void getStringReturnsDefaultForEmptyWhenDefIfEmptyEnabled() {
        SharedPreferenceUtils.setString(preferences, "string_key", "", true);
        Assert.assertEquals("def",
            SharedPreferenceUtils.getString(preferences, "string_key", "def", true));
    }

    @Test
    public void getStringReturnsEmptyWhenDefIfEmptyDisabled() {
        SharedPreferenceUtils.setString(preferences, "string_key", "", true);
        Assert.assertEquals("",
            SharedPreferenceUtils.getString(preferences, "string_key", "def", false));
    }

    @Test
    public void setStringIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setString(null, "string_key", "value", false);
        Assert.assertEquals("def",
            SharedPreferenceUtils.getString(preferences, "string_key", "def", true));
    }

    @Test
    public void getStringReturnsDefaultOnClassCastException() {
        preferences.edit().putInt("string_key", 5).commit();
        Assert.assertEquals("def",
            SharedPreferenceUtils.getString(preferences, "string_key", "def", true));
    }

    @Test
    public void getStringSetReturnsDefaultForNullPreferences() {
        Set<String> def = Collections.singleton("default");
        Assert.assertEquals(def, SharedPreferenceUtils.getStringSet(null, "key", def));
    }

    @Test
    public void setAndGetStringSetRoundTripsWithApplyAndCommit() {
        Set<String> values = new HashSet<>();
        values.add("a");
        values.add("b");
        SharedPreferenceUtils.setStringSet(preferences, "set_key", values, false);
        Assert.assertEquals(values,
            SharedPreferenceUtils.getStringSet(preferences, "set_key", Collections.emptySet()));

        Set<String> more = new HashSet<>();
        more.add("c");
        SharedPreferenceUtils.setStringSet(preferences, "set_key", more, true);
        Assert.assertEquals(more,
            SharedPreferenceUtils.getStringSet(preferences, "set_key", Collections.emptySet()));
    }

    @Test
    public void setStringSetIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setStringSet(null, "set_key", Collections.singleton("x"), false);
        Assert.assertTrue(
            SharedPreferenceUtils.getStringSet(preferences, "set_key", Collections.emptySet()).isEmpty());
    }

    @Test
    public void getStringSetReturnsDefaultOnClassCastException() {
        preferences.edit().putString("set_key", "not_a_set").commit();
        Set<String> def = Collections.singleton("default");
        Assert.assertEquals(def, SharedPreferenceUtils.getStringSet(preferences, "set_key", def));
    }

    @Test
    public void getIntStoredAsStringReturnsDefaultForNullPreferences() {
        Assert.assertEquals(13, SharedPreferenceUtils.getIntStoredAsString(null, "key", 13));
    }

    @Test
    public void setAndGetIntStoredAsStringRoundTripsWithApplyAndCommit() {
        SharedPreferenceUtils.setIntStoredAsString(preferences, "int_string_key", 17, false);
        Assert.assertEquals(17,
            SharedPreferenceUtils.getIntStoredAsString(preferences, "int_string_key", 0));
        SharedPreferenceUtils.setIntStoredAsString(preferences, "int_string_key", 29, true);
        Assert.assertEquals(29,
            SharedPreferenceUtils.getIntStoredAsString(preferences, "int_string_key", 0));
    }

    @Test
    public void setIntStoredAsStringIntoNullPreferencesIsIgnored() {
        SharedPreferenceUtils.setIntStoredAsString(null, "int_string_key", 5, false);
        Assert.assertEquals(0,
            SharedPreferenceUtils.getIntStoredAsString(preferences, "int_string_key", 0));
    }

    @Test
    public void getIntStoredAsStringReturnsDefaultForNonNumericValue() {
        preferences.edit().putString("int_string_key", "abc").commit();
        Assert.assertEquals(31,
            SharedPreferenceUtils.getIntStoredAsString(preferences, "int_string_key", 31));
    }

    @Test
    public void getIntStoredAsStringReturnsDefaultOnClassCastException() {
        preferences.edit().putInt("int_string_key", 5).commit();
        Assert.assertEquals(33,
            SharedPreferenceUtils.getIntStoredAsString(preferences, "int_string_key", 33));
    }
}
