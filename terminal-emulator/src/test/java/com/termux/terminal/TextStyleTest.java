package com.termux.terminal;

import junit.framework.TestCase;

public class TextStyleTest extends TestCase {

	private static final int[] ALL_EFFECTS = new int[]{0, TextStyle.CHARACTER_ATTRIBUTE_BOLD, TextStyle.CHARACTER_ATTRIBUTE_ITALIC,
			TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE, TextStyle.CHARACTER_ATTRIBUTE_BLINK, TextStyle.CHARACTER_ATTRIBUTE_INVERSE,
			TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE, TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH, TextStyle.CHARACTER_ATTRIBUTE_PROTECTED,
			TextStyle.CHARACTER_ATTRIBUTE_DIM};

	public void testEncodingSingle() {
		for (int fx : ALL_EFFECTS) {
			for (int fg = 0; fg < TextStyle.NUM_INDEXED_COLORS; fg++) {
				for (int bg = 0; bg < TextStyle.NUM_INDEXED_COLORS; bg++) {
					long encoded = TextStyle.encode(fg, bg, fx);
					assertEquals(fg, TextStyle.decodeForeColor(encoded));
					assertEquals(bg, TextStyle.decodeBackColor(encoded));
					assertEquals(fx, TextStyle.decodeEffect(encoded));
				}
			}
		}
	}

	public void testEncoding24Bit() {
		int[] values = {255, 240, 127, 1, 0};
		for (int red : values) {
			for (int green : values) {
				for (int blue : values) {
					int argb = 0xFF000000 | (red << 16) | (green << 8) | blue;
					long encoded = TextStyle.encode(argb, 0, 0);
					assertEquals(argb, TextStyle.decodeForeColor(encoded));
					encoded = TextStyle.encode(0, argb, 0);
					assertEquals(argb, TextStyle.decodeBackColor(encoded));
				}
			}
		}
	}


	public void testEncodingCombinations() {
		for (int f1 : ALL_EFFECTS) {
			for (int f2 : ALL_EFFECTS) {
				int combined = f1 | f2;
				assertEquals(combined, TextStyle.decodeEffect(TextStyle.encode(0, 0, combined)));
			}
		}
	}

	public void testEncodingStrikeThrough() {
		long encoded = TextStyle.encode(TextStyle.COLOR_INDEX_FOREGROUND, TextStyle.COLOR_INDEX_BACKGROUND,
				TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH);
		assertTrue((TextStyle.decodeEffect(encoded) & TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH) != 0);
	}

	public void testEncodingProtected() {
		long encoded = TextStyle.encode(TextStyle.COLOR_INDEX_FOREGROUND, TextStyle.COLOR_INDEX_BACKGROUND,
				TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH);
		assertEquals(0, (TextStyle.decodeEffect(encoded) & TextStyle.CHARACTER_ATTRIBUTE_PROTECTED));
		encoded = TextStyle.encode(TextStyle.COLOR_INDEX_FOREGROUND, TextStyle.COLOR_INDEX_BACKGROUND,
				TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH | TextStyle.CHARACTER_ATTRIBUTE_PROTECTED);
		assertTrue((TextStyle.decodeEffect(encoded) & TextStyle.CHARACTER_ATTRIBUTE_PROTECTED) != 0);
	}

	public void testNormalConstantDecodesToDefaults() {
		assertEquals(TextStyle.COLOR_INDEX_FOREGROUND, TextStyle.decodeForeColor(TextStyle.NORMAL));
		assertEquals(TextStyle.COLOR_INDEX_BACKGROUND, TextStyle.decodeBackColor(TextStyle.NORMAL));
		assertEquals(0, TextStyle.decodeEffect(TextStyle.NORMAL));
	}

	public void testForegroundAndBackgroundAreIndependent() {
		long encoded = TextStyle.encode(7, 200, 0);
		assertEquals(7, TextStyle.decodeForeColor(encoded));
		assertEquals(200, TextStyle.decodeBackColor(encoded));

		long swapped = TextStyle.encode(200, 7, 0);
		assertEquals(200, TextStyle.decodeForeColor(swapped));
		assertEquals(7, TextStyle.decodeBackColor(swapped));
	}

	public void testEffectsDoNotLeakIntoColors() {
		int allEffects = TextStyle.CHARACTER_ATTRIBUTE_BOLD | TextStyle.CHARACTER_ATTRIBUTE_ITALIC
				| TextStyle.CHARACTER_ATTRIBUTE_UNDERLINE | TextStyle.CHARACTER_ATTRIBUTE_BLINK
				| TextStyle.CHARACTER_ATTRIBUTE_INVERSE | TextStyle.CHARACTER_ATTRIBUTE_INVISIBLE
				| TextStyle.CHARACTER_ATTRIBUTE_STRIKETHROUGH | TextStyle.CHARACTER_ATTRIBUTE_PROTECTED
				| TextStyle.CHARACTER_ATTRIBUTE_DIM;
		long encoded = TextStyle.encode(11, 22, allEffects);
		assertEquals(11, TextStyle.decodeForeColor(encoded));
		assertEquals(22, TextStyle.decodeBackColor(encoded));
		assertEquals(allEffects, TextStyle.decodeEffect(encoded));
	}

	public void testTrueColorForegroundWithIndexedBackground() {
		int trueColorFg = 0xFF123456;
		long encoded = TextStyle.encode(trueColorFg, TextStyle.COLOR_INDEX_BACKGROUND,
				TextStyle.CHARACTER_ATTRIBUTE_BOLD);
		assertEquals(trueColorFg, TextStyle.decodeForeColor(encoded));
		assertEquals(TextStyle.COLOR_INDEX_BACKGROUND, TextStyle.decodeBackColor(encoded));
		assertTrue((TextStyle.decodeEffect(encoded) & TextStyle.CHARACTER_ATTRIBUTE_BOLD) != 0);
	}

	public void testTrueColorBothChannelsRoundTrip() {
		int fg = 0xFFAABBCC;
		int bg = 0xFF010203;
		long encoded = TextStyle.encode(fg, bg, 0);
		assertEquals(fg, TextStyle.decodeForeColor(encoded));
		assertEquals(bg, TextStyle.decodeBackColor(encoded));
	}

}
