package com.termux.terminal;

import junit.framework.TestCase;

public class TerminalColorsTest extends TestCase {

    private static int rgb(int r, int g, int b) {
        return 0xFF << 24 | r << 16 | g << 8 | b;
    }

    public void testParseHashRrggbb() {
        assertEquals(rgb(0x12, 0x34, 0x56), TerminalColors.parse("#123456"));
        assertEquals(rgb(0x00, 0x00, 0x00), TerminalColors.parse("#000000"));
        assertEquals(rgb(0xff, 0xff, 0xff), TerminalColors.parse("#ffffff"));
    }

    public void testParseHashRgbScalesSingleNibbleToByte() {
        assertEquals(rgb(0xff, 0x00, 0x00), TerminalColors.parse("#f00"));
        assertEquals(rgb(0x00, 0x00, 0x00), TerminalColors.parse("#000"));
        assertEquals(rgb(0xff, 0xff, 0xff), TerminalColors.parse("#fff"));
    }

    public void testParseHashRrrgggbbbScalesTwelveBitComponentToByte() {
        assertEquals(rgb(0xff, 0x00, 0x00), TerminalColors.parse("#fff000000"));
        assertEquals(rgb(0xff, 0xff, 0xff), TerminalColors.parse("#fffffffff"));
    }

    public void testParseHashRrrrggggbbbbScalesSixteenBitComponentToByte() {
        assertEquals(rgb(0xff, 0x00, 0x00), TerminalColors.parse("#ffff00000000"));
        assertEquals(rgb(0xff, 0xff, 0xff), TerminalColors.parse("#ffffffffffff"));
    }

    public void testParseRgbColonFormat() {
        assertEquals(rgb(0xff, 0x00, 0x00), TerminalColors.parse("rgb:ff/00/00"));
        assertEquals(rgb(0x12, 0x34, 0x56), TerminalColors.parse("rgb:12/34/56"));
    }

    public void testParseRgbColonSingleNibbleComponentsScaleToByte() {
        assertEquals(rgb(0xff, 0x00, 0x00), TerminalColors.parse("rgb:f/0/0"));
    }

    public void testParseReturnsZeroForUnknownPrefix() {
        assertEquals(0, TerminalColors.parse("blue"));
        assertEquals(0, TerminalColors.parse("0x123456"));
        assertEquals(0, TerminalColors.parse("rgba:ff/00/00"));
    }

    public void testParseReturnsZeroWhenComponentLengthsNotEqual() {
        assertEquals(0, TerminalColors.parse("#12345"));
        assertEquals(0, TerminalColors.parse("#1234567"));
    }

    public void testParseReturnsZeroForEmptyString() {
        assertEquals(0, TerminalColors.parse(""));
    }

    public void testParseReturnsZeroForNonHexDigits() {
        assertEquals(0, TerminalColors.parse("#gggggg"));
        assertEquals(0, TerminalColors.parse("rgb:zz/00/00"));
    }

    public void testResetRestoresDefaultColorsAfterModification() {
        TerminalColors colors = new TerminalColors();
        int defaultForeground = colors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND];

        colors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = rgb(0x11, 0x22, 0x33);
        assertFalse(defaultForeground == colors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND]);

        colors.reset();
        assertEquals(defaultForeground, colors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND]);
    }

    public void testResetIndexRestoresOnlyThatIndex() {
        TerminalColors colors = new TerminalColors();
        int defaultColorZero = colors.mCurrentColors[0];

        colors.mCurrentColors[0] = rgb(0x11, 0x22, 0x33);
        colors.mCurrentColors[1] = rgb(0x44, 0x55, 0x66);
        colors.reset(0);

        assertEquals(defaultColorZero, colors.mCurrentColors[0]);
        assertEquals(rgb(0x44, 0x55, 0x66), colors.mCurrentColors[1]);
    }

    public void testTryParseColorStoresParsedColorAtIndex() {
        TerminalColors colors = new TerminalColors();
        colors.tryParseColor(5, "#abcdef");
        assertEquals(rgb(0xab, 0xcd, 0xef), colors.mCurrentColors[5]);
    }

    public void testTryParseColorLeavesIndexUnchangedWhenParseFails() {
        TerminalColors colors = new TerminalColors();
        int original = colors.mCurrentColors[5];
        colors.tryParseColor(5, "not-a-color");
        assertEquals(original, colors.mCurrentColors[5]);
    }

}
