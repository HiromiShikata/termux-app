package com.termux.shared.data;

import org.junit.Assert;
import org.junit.Test;

public class DataUtilsTest {

    @Test
    public void getTruncatedCommandOutputReturnsNullForNullInput() {
        Assert.assertNull(DataUtils.getTruncatedCommandOutput(null, 10, true, false, false));
    }

    @Test
    public void getTruncatedCommandOutputReturnsOriginalWhenShorterThanMax() {
        Assert.assertEquals("short", DataUtils.getTruncatedCommandOutput("short", 100, true, false, false));
    }

    @Test
    public void getTruncatedCommandOutputFromStartKeepsLeadingCharacters() {
        Assert.assertEquals("0123", DataUtils.getTruncatedCommandOutput("0123456789", 4, true, false, false));
    }

    @Test
    public void getTruncatedCommandOutputFromEndKeepsTrailingCharacters() {
        Assert.assertEquals("6789", DataUtils.getTruncatedCommandOutput("0123456789", 4, false, false, false));
    }

    @Test
    public void getTruncatedCommandOutputWithPrefixPrependsTruncatedMarker() {
        String result = DataUtils.getTruncatedCommandOutput("0123456789", 16, true, false, true);
        Assert.assertTrue(result.startsWith("(truncated) "));
    }

    @Test
    public void getTruncatedCommandOutputOnNewlineCutsAtNextNewline() {
        String result = DataUtils.getTruncatedCommandOutput("aaaa\nbbbb\ncccc", 8, false, true, false);
        Assert.assertEquals("cccc", result);
    }

    @Test
    public void getTruncatedCommandOutputNegativeMaxReturnsOriginal() {
        Assert.assertEquals("data", DataUtils.getTruncatedCommandOutput("data", -1, true, false, false));
    }

    @Test
    public void replaceSubStringsInStringArrayItemsReplacesInEachItem() {
        String[] array = {"a-b", "c-d"};
        DataUtils.replaceSubStringsInStringArrayItems(array, "-", "_");
        Assert.assertArrayEquals(new String[]{"a_b", "c_d"}, array);
    }

    @Test
    public void replaceSubStringsInStringArrayItemsHandlesNullArray() {
        DataUtils.replaceSubStringsInStringArrayItems(null, "-", "_");
    }

    @Test
    public void replaceSubStringsInStringArrayItemsHandlesEmptyArray() {
        String[] array = new String[0];
        DataUtils.replaceSubStringsInStringArrayItems(array, "-", "_");
        Assert.assertEquals(0, array.length);
    }

    @Test
    public void getFloatFromStringParsesValidValue() {
        Assert.assertEquals(3.5f, DataUtils.getFloatFromString("3.5", 0f), 0.0001f);
    }

    @Test
    public void getFloatFromStringReturnsDefaultForNull() {
        Assert.assertEquals(1.0f, DataUtils.getFloatFromString(null, 1.0f), 0.0001f);
    }

    @Test
    public void getFloatFromStringReturnsDefaultForInvalidValue() {
        Assert.assertEquals(2.0f, DataUtils.getFloatFromString("not-a-number", 2.0f), 0.0001f);
    }

    @Test
    public void getIntFromStringParsesValidValue() {
        Assert.assertEquals(42, DataUtils.getIntFromString("42", 0));
    }

    @Test
    public void getIntFromStringReturnsDefaultForNull() {
        Assert.assertEquals(7, DataUtils.getIntFromString(null, 7));
    }

    @Test
    public void getIntFromStringReturnsDefaultForInvalidValue() {
        Assert.assertEquals(9, DataUtils.getIntFromString("12x", 9));
    }

    @Test
    public void getStringFromIntegerReturnsValueWhenPresent() {
        Assert.assertEquals("15", DataUtils.getStringFromInteger(15, "def"));
    }

    @Test
    public void getStringFromIntegerReturnsDefaultWhenNull() {
        Assert.assertEquals("def", DataUtils.getStringFromInteger(null, "def"));
    }

    @Test
    public void bytesToHexProducesUppercaseHex() {
        Assert.assertEquals("00FF10", DataUtils.bytesToHex(new byte[]{0x00, (byte) 0xFF, 0x10}));
    }

    @Test
    public void bytesToHexOnEmptyArrayReturnsEmptyString() {
        Assert.assertEquals("", DataUtils.bytesToHex(new byte[0]));
    }

    @Test
    public void clampReturnsValueWithinRange() {
        Assert.assertEquals(5, DataUtils.clamp(5, 0, 10));
    }

    @Test
    public void clampLimitsToMinimum() {
        Assert.assertEquals(0, DataUtils.clamp(-3, 0, 10));
    }

    @Test
    public void clampLimitsToMaximum() {
        Assert.assertEquals(10, DataUtils.clamp(99, 0, 10));
    }

    @Test
    public void rangedOrDefaultReturnsValueWithinRange() {
        Assert.assertEquals(5.0f, DataUtils.rangedOrDefault(5.0f, 1.0f, 0.0f, 10.0f), 0.0001f);
    }

    @Test
    public void rangedOrDefaultReturnsDefaultBelowMin() {
        Assert.assertEquals(1.0f, DataUtils.rangedOrDefault(-1.0f, 1.0f, 0.0f, 10.0f), 0.0001f);
    }

    @Test
    public void rangedOrDefaultReturnsDefaultAboveMax() {
        Assert.assertEquals(1.0f, DataUtils.rangedOrDefault(20.0f, 1.0f, 0.0f, 10.0f), 0.0001f);
    }

    @Test
    public void getSpaceIndentedStringIndentsWithFourSpaces() {
        Assert.assertEquals("    line", DataUtils.getSpaceIndentedString("line", 1));
    }

    @Test
    public void getSpaceIndentedStringReturnsEmptyForEmptyInput() {
        Assert.assertEquals("", DataUtils.getSpaceIndentedString("", 2));
    }

    @Test
    public void getSpaceIndentedStringReturnsNullForNullInput() {
        Assert.assertNull(DataUtils.getSpaceIndentedString(null, 2));
    }

    @Test
    public void getTabIndentedStringIndentsWithTab() {
        Assert.assertEquals("\tline", DataUtils.getTabIndentedString("line", 1));
    }

    @Test
    public void getIndentedStringIndentsEveryLine() {
        Assert.assertEquals(">>a\n>>b", DataUtils.getIndentedString("a\nb", ">", 2));
    }

    @Test
    public void getIndentedStringTreatsCountBelowOneAsOne() {
        Assert.assertEquals(">line", DataUtils.getIndentedString("line", ">", 0));
    }

    @Test
    public void getDefaultIfNullReturnsObjectWhenNotNull() {
        Assert.assertEquals("value", DataUtils.getDefaultIfNull("value", "def"));
    }

    @Test
    public void getDefaultIfNullReturnsDefaultWhenNull() {
        Assert.assertEquals("def", DataUtils.getDefaultIfNull(null, "def"));
    }

    @Test
    public void getDefaultIfUnsetReturnsValueWhenPresent() {
        Assert.assertEquals("value", DataUtils.getDefaultIfUnset("value", "def"));
    }

    @Test
    public void getDefaultIfUnsetReturnsDefaultWhenNull() {
        Assert.assertEquals("def", DataUtils.getDefaultIfUnset(null, "def"));
    }

    @Test
    public void getDefaultIfUnsetReturnsDefaultWhenEmpty() {
        Assert.assertEquals("def", DataUtils.getDefaultIfUnset("", "def"));
    }

    @Test
    public void isNullOrEmptyTrueForNull() {
        Assert.assertTrue(DataUtils.isNullOrEmpty(null));
    }

    @Test
    public void isNullOrEmptyTrueForEmpty() {
        Assert.assertTrue(DataUtils.isNullOrEmpty(""));
    }

    @Test
    public void isNullOrEmptyFalseForNonEmpty() {
        Assert.assertFalse(DataUtils.isNullOrEmpty("x"));
    }

    @Test
    public void getSerializedSizeReturnsZeroForNull() {
        Assert.assertEquals(0, DataUtils.getSerializedSize(null));
    }

    @Test
    public void getSerializedSizeReturnsPositiveForSerializableObject() {
        Assert.assertTrue(DataUtils.getSerializedSize("hello") > 0);
    }

    @Test
    public void getTruncatedCommandOutputWithPrefixReturnsTruncatedTail() {
        String result = DataUtils.getTruncatedCommandOutput("0123456789", 18, false, false, true);
        Assert.assertTrue(result.startsWith("(truncated) "));
        Assert.assertTrue(result.endsWith("789"));
    }

    @Test
    public void getTruncatedCommandOutputOnNewlineWhenNoFurtherNewlineKeepsTailFromCutOff() {
        String result = DataUtils.getTruncatedCommandOutput("aaaa\nbbbbbbbb", 4, false, true, false);
        Assert.assertEquals("bbbb", result);
    }

    @Test
    public void getTruncatedCommandOutputOnNewlineIgnoresTrailingNewline() {
        String result = DataUtils.getTruncatedCommandOutput("aaaaaaaa\n", 5, false, true, false);
        Assert.assertEquals("aaaa\n", result);
    }

    @Test
    public void getSerializedSizeReturnsNegativeOneForNonSerializableGraph() {
        Assert.assertEquals(-1, DataUtils.getSerializedSize(new java.util.ArrayList<Object>() {{
            add(new Object());
        }}));
    }

    @Test
    public void getStringFromIntegerFormatsValueAsBaseTen() {
        Assert.assertEquals("-8", DataUtils.getStringFromInteger(-8, "def"));
    }

    @Test
    public void getIndentedStringReturnsNullForNullInput() {
        Assert.assertNull(DataUtils.getIndentedString(null, ">", 1));
    }
}
