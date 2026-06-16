package com.termux.shared.data;

import android.content.Intent;
import android.os.Bundle;

import com.termux.shared.logger.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class IntentUtilsTest {

    private static class StubIntent extends Intent {
        private final Map<String, Object> extras = new HashMap<>();
        private Bundle extrasBundle;
        private String stringRepresentation = "Intent{}";

        StubIntent putStringExtra(String key, String value) {
            extras.put(key, value);
            return this;
        }

        StubIntent putStringArrayExtra(String key, String[] value) {
            extras.put(key, value);
            return this;
        }

        StubIntent withExtras(Bundle bundle) {
            this.extrasBundle = bundle;
            return this;
        }

        StubIntent withStringRepresentation(String value) {
            this.stringRepresentation = value;
            return this;
        }

        @Override
        public String getStringExtra(String name) {
            Object value = extras.get(name);
            return value instanceof String ? (String) value : null;
        }

        @Override
        public String[] getStringArrayExtra(String name) {
            Object value = extras.get(name);
            return value instanceof String[] ? (String[]) value : null;
        }

        @Override
        public Bundle getExtras() {
            return extrasBundle;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }

    @Before
    public void disableAndroidLogging() {
        Logger.setLogLevel(null, Logger.LOG_LEVEL_OFF);
    }

    @Test
    public void getStringExtraIfSetReturnsValueWhenPresent() {
        StubIntent intent = new StubIntent().putStringExtra("key", "value");
        Assert.assertEquals("value", IntentUtils.getStringExtraIfSet(intent, "key", "default"));
    }

    @Test
    public void getStringExtraIfSetReturnsDefaultWhenValueMissing() {
        Assert.assertEquals("default", IntentUtils.getStringExtraIfSet(new StubIntent(), "key", "default"));
    }

    @Test
    public void getStringExtraIfSetReturnsDefaultWhenValueEmpty() {
        StubIntent intent = new StubIntent().putStringExtra("key", "");
        Assert.assertEquals("default", IntentUtils.getStringExtraIfSet(intent, "key", "default"));
    }

    @Test
    public void getStringExtraIfSetReturnsNullWhenMissingAndDefaultNull() {
        Assert.assertNull(IntentUtils.getStringExtraIfSet(new StubIntent(), "key", null));
    }

    @Test
    public void getStringExtraIfSetReturnsNullWhenMissingAndDefaultEmpty() {
        Assert.assertNull(IntentUtils.getStringExtraIfSet(new StubIntent(), "key", ""));
    }

    @Test
    public void getStringExtraIfSetWithThrowFlagReturnsValueWhenSet() throws Exception {
        StubIntent intent = new StubIntent().putStringExtra("key", "value");
        Assert.assertEquals("value", IntentUtils.getStringExtraIfSet(intent, "key", null, true));
    }

    @Test
    public void getStringExtraIfSetWithThrowFlagThrowsWhenNotSet() {
        try {
            IntentUtils.getStringExtraIfSet(new StubIntent(), "missing", null, true);
            Assert.fail("Expected an exception when the extra is not set");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("missing"));
        }
    }

    @Test
    public void getStringExtraIfSetWithThrowFlagDoesNotThrowWhenDefaultUsed() throws Exception {
        Assert.assertEquals("default", IntentUtils.getStringExtraIfSet(new StubIntent(), "missing", "default", true));
    }

    @Test
    public void getIntegerExtraIfSetParsesNumericString() {
        StubIntent intent = new StubIntent().putStringExtra("num", "42");
        Assert.assertEquals(Integer.valueOf(42), IntentUtils.getIntegerExtraIfSet(intent, "num", 0));
    }

    @Test
    public void getIntegerExtraIfSetParsesNegativeNumericString() {
        StubIntent intent = new StubIntent().putStringExtra("num", "-7");
        Assert.assertEquals(Integer.valueOf(-7), IntentUtils.getIntegerExtraIfSet(intent, "num", 0));
    }

    @Test
    public void getIntegerExtraIfSetReturnsDefaultWhenMissing() {
        Assert.assertEquals(Integer.valueOf(99), IntentUtils.getIntegerExtraIfSet(new StubIntent(), "num", 99));
    }

    @Test
    public void getIntegerExtraIfSetReturnsDefaultWhenEmpty() {
        StubIntent intent = new StubIntent().putStringExtra("num", "");
        Assert.assertEquals(Integer.valueOf(5), IntentUtils.getIntegerExtraIfSet(intent, "num", 5));
    }

    @Test
    public void getIntegerExtraIfSetReturnsDefaultWhenNotParsable() {
        StubIntent intent = new StubIntent().putStringExtra("num", "not-a-number");
        Assert.assertEquals(Integer.valueOf(3), IntentUtils.getIntegerExtraIfSet(intent, "num", 3));
    }

    @Test
    public void getIntegerExtraIfSetReturnsNullDefaultWhenMissing() {
        Assert.assertNull(IntentUtils.getIntegerExtraIfSet(new StubIntent(), "num", null));
    }

    @Test
    public void getStringArrayExtraIfSetReturnsValueWhenPresent() {
        String[] expected = {"a", "b"};
        StubIntent intent = new StubIntent().putStringArrayExtra("arr", expected);
        Assert.assertArrayEquals(expected, IntentUtils.getStringArrayExtraIfSet(intent, "arr", null));
    }

    @Test
    public void getStringArrayExtraIfSetReturnsDefaultWhenMissing() {
        String[] def = {"x"};
        Assert.assertArrayEquals(def, IntentUtils.getStringArrayExtraIfSet(new StubIntent(), "arr", def));
    }

    @Test
    public void getStringArrayExtraIfSetReturnsDefaultWhenEmpty() {
        String[] def = {"x"};
        StubIntent intent = new StubIntent().putStringArrayExtra("arr", new String[0]);
        Assert.assertArrayEquals(def, IntentUtils.getStringArrayExtraIfSet(intent, "arr", def));
    }

    @Test
    public void getStringArrayExtraIfSetReturnsNullWhenMissingAndDefaultNull() {
        Assert.assertNull(IntentUtils.getStringArrayExtraIfSet(new StubIntent(), "arr", null));
    }

    @Test
    public void getStringArrayExtraIfSetReturnsNullWhenMissingAndDefaultEmpty() {
        Assert.assertNull(IntentUtils.getStringArrayExtraIfSet(new StubIntent(), "arr", new String[0]));
    }

    @Test
    public void getStringArrayExtraIfSetWithThrowFlagReturnsValueWhenSet() throws Exception {
        String[] expected = {"a"};
        StubIntent intent = new StubIntent().putStringArrayExtra("arr", expected);
        Assert.assertArrayEquals(expected, IntentUtils.getStringArrayExtraIfSet(intent, "arr", null, true));
    }

    @Test
    public void getStringArrayExtraIfSetWithThrowFlagThrowsWhenNotSet() {
        try {
            IntentUtils.getStringArrayExtraIfSet(new StubIntent(), "missing", null, true);
            Assert.fail("Expected an exception when the array extra is not set");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("missing"));
        }
    }

    @Test
    public void getBundleStringReturnsEmptyMarkerForNullBundle() {
        Assert.assertEquals("Bundle[]", IntentUtils.getBundleString(null));
    }

    @Test
    public void getIntentStringReturnsNullForNullIntent() {
        Assert.assertNull(IntentUtils.getIntentString(null));
    }

    @Test
    public void getIntentStringCombinesToStringAndBundle() {
        StubIntent intent = new StubIntent().withStringRepresentation("Intent{action=test}").withExtras(null);
        Assert.assertEquals("Intent{action=test}\nBundle[]", IntentUtils.getIntentString(intent));
    }
}
