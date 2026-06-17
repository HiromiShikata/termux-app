package com.termux.shared.settings.properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(RobolectricTestRunner.class)
public class SharedPropertiesValueTest {

    private static final String LOG_TAG = "SharedPropertiesValueTest";

    @Test
    public void genericBooleanMapHoldsExpectedEntries() {
        Assert.assertEquals(Boolean.TRUE, SharedProperties.MAP_GENERIC_BOOLEAN.get("true"));
        Assert.assertEquals(Boolean.FALSE, SharedProperties.MAP_GENERIC_BOOLEAN.get("false"));
    }

    @Test
    public void genericInvertedBooleanMapHoldsExpectedEntries() {
        Assert.assertEquals(Boolean.FALSE, SharedProperties.MAP_GENERIC_INVERTED_BOOLEAN.get("true"));
        Assert.assertEquals(Boolean.TRUE, SharedProperties.MAP_GENERIC_INVERTED_BOOLEAN.get("false"));
    }

    @Test
    public void getBooleanValueForStringValueMapsLiterals() {
        Assert.assertEquals(Boolean.TRUE, SharedProperties.getBooleanValueForStringValue("true"));
        Assert.assertEquals(Boolean.FALSE, SharedProperties.getBooleanValueForStringValue("false"));
        Assert.assertEquals(Boolean.TRUE, SharedProperties.getBooleanValueForStringValue("TRUE"));
    }

    @Test
    public void getBooleanValueForStringValueReturnsNullForUnknown() {
        Assert.assertNull(SharedProperties.getBooleanValueForStringValue("maybe"));
        Assert.assertNull(SharedProperties.getBooleanValueForStringValue(null));
    }

    @Test
    public void getBooleanValueForStringValueWithDefaultParsesKnownValues() {
        Assert.assertTrue(SharedProperties.getBooleanValueForStringValue("key", "true", false, false, LOG_TAG));
        Assert.assertFalse(SharedProperties.getBooleanValueForStringValue("key", "false", true, false, LOG_TAG));
    }

    @Test
    public void getBooleanValueForStringValueWithDefaultFallsBackForUnknown() {
        Assert.assertTrue(SharedProperties.getBooleanValueForStringValue("key", "unknown", true, true, LOG_TAG));
        Assert.assertFalse(SharedProperties.getBooleanValueForStringValue("key", null, false, false, LOG_TAG));
    }

    @Test
    public void getInvertedBooleanValueForStringValueInvertsKnownValues() {
        Assert.assertFalse(SharedProperties.getInvertedBooleanValueForStringValue("key", "true", true, false, LOG_TAG));
        Assert.assertTrue(SharedProperties.getInvertedBooleanValueForStringValue("key", "false", false, false, LOG_TAG));
    }

    @Test
    public void getInvertedBooleanValueForStringValueFallsBackForUnknown() {
        Assert.assertTrue(SharedProperties.getInvertedBooleanValueForStringValue("key", "unknown", true, true, LOG_TAG));
    }

    @Test
    public void getDefaultIfNotInMapReturnsMappedValue() {
        Object value = SharedProperties.getDefaultIfNotInMap("key", SharedProperties.MAP_GENERIC_BOOLEAN,
            "true", false, false, LOG_TAG);
        Assert.assertEquals(Boolean.TRUE, value);
    }

    @Test
    public void getDefaultIfNotInMapReturnsDefaultForMissingKey() {
        Object value = SharedProperties.getDefaultIfNotInMap("key", SharedProperties.MAP_GENERIC_BOOLEAN,
            "missing", false, true, LOG_TAG);
        Assert.assertEquals(Boolean.FALSE, value);
    }

    @Test
    public void getDefaultIfNotInRangeIntReturnsValueInRange() {
        Assert.assertEquals(5, SharedProperties.getDefaultIfNotInRange("key", 5, 1, 0, 10, false, false, LOG_TAG));
    }

    @Test
    public void getDefaultIfNotInRangeIntReturnsDefaultBelowMin() {
        Assert.assertEquals(1, SharedProperties.getDefaultIfNotInRange("key", -5, 1, 0, 10, true, false, LOG_TAG));
    }

    @Test
    public void getDefaultIfNotInRangeIntReturnsDefaultAboveMax() {
        Assert.assertEquals(1, SharedProperties.getDefaultIfNotInRange("key", 50, 1, 0, 10, true, true, LOG_TAG));
    }

    @Test
    public void getDefaultIfNotInRangeFloatReturnsValueInRange() {
        Assert.assertEquals(2.5f, SharedProperties.getDefaultIfNotInRange("key", 2.5f, 1.0f, 0.0f, 5.0f, false, false, LOG_TAG), 0.0001f);
    }

    @Test
    public void getDefaultIfNotInRangeFloatReturnsDefaultOutOfRange() {
        Assert.assertEquals(1.0f, SharedProperties.getDefaultIfNotInRange("key", 9.0f, 1.0f, 0.0f, 5.0f, true, false, LOG_TAG), 0.0001f);
    }

    @Test
    public void getDefaultIfNullReturnsObjectWhenPresent() {
        Assert.assertEquals("value", SharedProperties.getDefaultIfNull("value", "def"));
    }

    @Test
    public void getDefaultIfNullReturnsDefaultWhenNull() {
        Assert.assertEquals("def", SharedProperties.getDefaultIfNull(null, "def"));
    }

    @Test
    public void getDefaultIfNullOrEmptyReturnsDefaultForNullAndEmpty() {
        Assert.assertEquals("def", SharedProperties.getDefaultIfNullOrEmpty(null, "def"));
        Assert.assertEquals("def", SharedProperties.getDefaultIfNullOrEmpty("", "def"));
    }

    @Test
    public void getDefaultIfNullOrEmptyReturnsValueWhenPresent() {
        Assert.assertEquals("value", SharedProperties.getDefaultIfNullOrEmpty("value", "def"));
    }

    @Test
    public void toLowerCaseHandlesNull() {
        Assert.assertNull(SharedProperties.toLowerCase(null));
        Assert.assertEquals("abc", SharedProperties.toLowerCase("ABC"));
    }

    @Test
    public void putToMapRejectsNullMap() {
        Assert.assertFalse(SharedProperties.putToMap(null, "key", "value"));
    }

    @Test
    public void putToMapRejectsNullKey() {
        HashMap<String, Object> map = new HashMap<>();
        Assert.assertFalse(SharedProperties.putToMap(map, null, "value"));
    }

    @Test
    public void putToMapStoresPrimitiveAndStringValues() {
        HashMap<String, Object> map = new HashMap<>();
        Assert.assertTrue(SharedProperties.putToMap(map, "string", "value"));
        Assert.assertTrue(SharedProperties.putToMap(map, "int", 5));
        Assert.assertTrue(SharedProperties.putToMap(map, "bool", true));
        Assert.assertTrue(SharedProperties.putToMap(map, "null", null));
        Assert.assertEquals("value", map.get("string"));
        Assert.assertEquals(5, map.get("int"));
        Assert.assertEquals(true, map.get("bool"));
        Assert.assertNull(map.get("null"));
    }

    @Test
    public void putToMapRejectsNonPrimitiveValue() {
        HashMap<String, Object> map = new HashMap<>();
        Assert.assertFalse(SharedProperties.putToMap(map, "object", new Object()));
        Assert.assertFalse(map.containsKey("object"));
    }

    @Test
    public void putToPropertiesRejectsNullProperties() {
        Assert.assertFalse(SharedProperties.putToProperties(null, "key", "value"));
    }

    @Test
    public void putToPropertiesRejectsNullKey() {
        Properties properties = new Properties();
        Assert.assertFalse(SharedProperties.putToProperties(properties, null, "value"));
    }

    @Test
    public void putToPropertiesStoresValue() {
        Properties properties = new Properties();
        Assert.assertTrue(SharedProperties.putToProperties(properties, "key", "value"));
        Assert.assertEquals("value", properties.get("key"));
    }

    @Test
    public void putToPropertiesRemovesKeyForNullValue() {
        Properties properties = new Properties();
        properties.put("key", "value");
        Assert.assertTrue(SharedProperties.putToProperties(properties, "key", null));
        Assert.assertFalse(properties.containsKey("key"));
    }

    @Test
    public void getPropertiesCopyReturnsNullForNull() {
        Assert.assertNull(SharedProperties.getPropertiesCopy(null));
    }

    @Test
    public void getPropertiesCopyDuplicatesEntries() {
        Properties source = new Properties();
        source.put("key", "value");
        Properties copy = SharedProperties.getPropertiesCopy(source);
        Assert.assertNotSame(source, copy);
        Assert.assertEquals("value", copy.get("key"));
    }

    @Test
    public void getMapCopyReturnsNullForNull() {
        Assert.assertNull(SharedProperties.getMapCopy(null));
    }

    @Test
    public void getMapCopyDuplicatesEntries() {
        Map<String, Object> source = new HashMap<>();
        source.put("key", "value");
        Map<String, Object> copy = SharedProperties.getMapCopy(source);
        Assert.assertNotSame(source, copy);
        Assert.assertEquals("value", copy.get("key"));
    }
}
