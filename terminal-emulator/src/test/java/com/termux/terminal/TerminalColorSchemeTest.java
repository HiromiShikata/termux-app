package com.termux.terminal;

import junit.framework.TestCase;

import java.util.Properties;

public class TerminalColorSchemeTest extends TestCase {

    private static int rgb(int r, int g, int b) {
        return 0xFF << 24 | r << 16 | g << 8 | b;
    }

    private static Properties propertiesOf(String... keyValuePairs) {
        Properties properties = new Properties();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            properties.setProperty(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return properties;
    }

    public void testNewSchemeHasDefaultColors() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        assertEquals(TextStyle.NUM_INDEXED_COLORS, scheme.mDefaultColors.length);
        assertEquals(0xff000000, scheme.mDefaultColors[0]);
        assertEquals(0xffffffff, scheme.mDefaultColors[TextStyle.COLOR_INDEX_FOREGROUND]);
        assertEquals(0xff000000, scheme.mDefaultColors[TextStyle.COLOR_INDEX_BACKGROUND]);
    }

    public void testUpdateWithForegroundBackgroundAndCursorKeys() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        scheme.updateWith(propertiesOf(
            "foreground", "#112233",
            "background", "#445566",
            "cursor", "#778899"));

        assertEquals(rgb(0x11, 0x22, 0x33), scheme.mDefaultColors[TextStyle.COLOR_INDEX_FOREGROUND]);
        assertEquals(rgb(0x44, 0x55, 0x66), scheme.mDefaultColors[TextStyle.COLOR_INDEX_BACKGROUND]);
        assertEquals(rgb(0x77, 0x88, 0x99), scheme.mDefaultColors[TextStyle.COLOR_INDEX_CURSOR]);
    }

    public void testUpdateWithIndexedColorKey() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        scheme.updateWith(propertiesOf(
            "color3", "#abcdef",
            "cursor", "#000000"));
        assertEquals(rgb(0xab, 0xcd, 0xef), scheme.mDefaultColors[3]);
    }

    public void testUpdateWithRgbColonValueFormat() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        scheme.updateWith(propertiesOf(
            "color10", "rgb:12/34/56",
            "cursor", "#000000"));
        assertEquals(rgb(0x12, 0x34, 0x56), scheme.mDefaultColors[10]);
    }

    public void testUpdateWithResetsPreviouslyOverriddenColorsNotInNewProperties() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        int defaultColorTwo = scheme.mDefaultColors[2];

        scheme.updateWith(propertiesOf("color2", "#010203", "cursor", "#000000"));
        assertEquals(rgb(0x01, 0x02, 0x03), scheme.mDefaultColors[2]);

        scheme.updateWith(propertiesOf("color5", "#040506", "cursor", "#000000"));
        assertEquals(defaultColorTwo, scheme.mDefaultColors[2]);
        assertEquals(rgb(0x04, 0x05, 0x06), scheme.mDefaultColors[5]);
    }

    public void testUpdateWithEmptyPropertiesKeepsDefaults() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        int defaultForeground = scheme.mDefaultColors[TextStyle.COLOR_INDEX_FOREGROUND];
        scheme.updateWith(propertiesOf("cursor", "#000000"));
        assertEquals(defaultForeground, scheme.mDefaultColors[TextStyle.COLOR_INDEX_FOREGROUND]);
    }

    public void testUpdateWithThrowsForUnknownKey() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        try {
            scheme.updateWith(propertiesOf("foobar", "#112233"));
            fail("Expected IllegalArgumentException for unknown property key");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foobar"));
        }
    }

    public void testUpdateWithThrowsForColorKeyWithNonNumericIndex() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        try {
            scheme.updateWith(propertiesOf("colorXY", "#112233"));
            fail("Expected IllegalArgumentException for non-numeric color index");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("colorXY"));
        }
    }

    public void testUpdateWithThrowsForInvalidColorValue() {
        TerminalColorScheme scheme = new TerminalColorScheme();
        try {
            scheme.updateWith(propertiesOf("foreground", "not-a-color"));
            fail("Expected IllegalArgumentException for invalid color value");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("foreground"));
        }
    }

}
