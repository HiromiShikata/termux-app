package com.termux.terminal;

import junit.framework.TestCase;

public class WcWidthTest extends TestCase {

	private static void assertWidthIs(int expectedWidth, int codePoint) {
		int wcWidth = WcWidth.width(codePoint);
		assertEquals(expectedWidth, wcWidth);
	}

	public void testPrintableAscii() {
		for (int i = 0x20; i <= 0x7E; i++) {
			assertWidthIs(1, i);
		}
	}

	public void testSomeWidthOne() {
		assertWidthIs(1, 'å');
		assertWidthIs(1, 'ä');
		assertWidthIs(1, 'ö');
		assertWidthIs(1, 0x23F2);
	}

	public void testSomeWide() {
		assertWidthIs(2, 'Ａ');
		assertWidthIs(2, 'Ｂ');
		assertWidthIs(2, 'Ｃ');
		assertWidthIs(2, '中');
		assertWidthIs(2, '文');

		assertWidthIs(2, 0x679C);
		assertWidthIs(2, 0x679D);

		assertWidthIs(2, 0x2070E);
		assertWidthIs(2, 0x20731);

		assertWidthIs(1, 0x1F781);
	}

	public void testSomeNonWide() {
		assertWidthIs(1, 0x1D11E);
		assertWidthIs(1, 0x1D11F);
	}

	public void testCombining() {
		assertWidthIs(0, 0x0302);
		assertWidthIs(0, 0x0308);
		assertWidthIs(0, 0xFE0F);
	}

	public void testWordJoiner() {
		// https://en.wikipedia.org/wiki/Word_joiner
		// The word joiner (WJ) is a code point in Unicode used to separate words when using scripts
		// that do not use explicit spacing. It is encoded since Unicode version 3.2
		// (released in 2002) as U+2060 WORD JOINER (HTML &#8288;).
		// The word joiner does not produce any space, and prohibits a line break at its position.
		assertWidthIs(0, 0x2060);
	}

	public void testSofthyphen() {
		// http://osdir.com/ml/internationalization.linux/2003-05/msg00006.html:
		// "Existing implementation practice in terminals is that the SOFT HYPHEN is
		// a spacing graphical character, and the purpose of my wcwidth() was to
		// predict the advancement of the cursor position after a string is sent to
		// a terminal. Hence, I have no choice but to keep wcwidth(SOFT HYPHEN) = 1.
		// VT100-style terminals do not hyphenate."
		assertWidthIs(1, 0x00AD);
	}

	public void testHangul() {
		assertWidthIs(1, 0x11A3);
	}

	public void testEmojis() {
		assertWidthIs(2, 0x1F428); // KOALA.
		assertWidthIs(2, 0x231a);  // WATCH.
		assertWidthIs(2, 0x1F643); // UPSIDE-DOWN FACE (Unicode 8).
	}

	public void testNullAndControlCharactersAreZeroWidth() {
		assertWidthIs(0, 0x0000);
		for (int i = 1; i < 0x20; i++) {
			assertWidthIs(0, i);
		}
		assertWidthIs(0, 0x007F);
		assertWidthIs(0, 0x0080);
		assertWidthIs(0, 0x009F);
	}

	public void testExplicitlyHandledZeroWidthCodePoints() {
		assertWidthIs(0, 0x034F); // Combining grapheme joiner.
		assertWidthIs(0, 0x200B); // Zero width space.
		assertWidthIs(0, 0x200F); // Right-to-left mark.
		assertWidthIs(0, 0x2028); // Line separator.
		assertWidthIs(0, 0x2029); // Paragraph separator.
		assertWidthIs(0, 0x202A); // Left-to-right embedding.
		assertWidthIs(0, 0x2063); // Invisible separator.
	}

	public void testZeroWidthRangeBoundaries() {
		assertWidthIs(0, 0x0300); // First combining grave accent.
		assertWidthIs(0, 0x036F); // Last in that range.
		assertWidthIs(1, 0x02FF); // Just before the range.
		assertWidthIs(1, 0x0370); // Just after the range.
	}

	public void testWideRangeBoundaries() {
		assertWidthIs(2, 0x1100); // First Hangul Choseong.
		assertWidthIs(2, 0x115F); // Last Hangul Choseong.
		assertWidthIs(1, 0x10FF); // Just before the wide range.
		assertWidthIs(1, 0x1160); // Just after the wide range.
		assertWidthIs(2, 0x4E00); // First CJK unified ideograph.
		assertWidthIs(2, 0x9FFF); // Within CJK unified ideographs.
	}

	public void testWidthFromCharArrayForBmpAndSurrogatePair() {
		char[] bmp = {'中'};
		assertEquals(2, WcWidth.width(bmp, 0));

		char[] ascii = {'a'};
		assertEquals(1, WcWidth.width(ascii, 0));

		char[] surrogatePair = Character.toChars(0x1F428); // KOALA, two java chars.
		assertEquals(2, surrogatePair.length);
		assertEquals(2, WcWidth.width(surrogatePair, 0));
	}

	public void testZeroWidthCharsCount() {
		char[] chars = {'a', 0x0308, 0x0301, 'b'};
		assertEquals(2, WcWidth.zeroWidthCharsCount(chars, 0, chars.length));
		assertEquals(1, WcWidth.zeroWidthCharsCount(chars, 1, 2));
		assertEquals(0, WcWidth.zeroWidthCharsCount(chars, 3, 4));
	}

	public void testZeroWidthCharsCountWithSurrogatePair() {
		char[] combiningSurrogate = Character.toChars(0xE0100); // Variation selector, zero width.
		assertEquals(2, combiningSurrogate.length);
		char[] chars = {'a', combiningSurrogate[0], combiningSurrogate[1]};
		assertEquals(1, WcWidth.zeroWidthCharsCount(chars, 0, chars.length));
	}

	public void testZeroWidthCharsCountWithOutOfRangeStartReturnsZero() {
		char[] chars = {'a', 0x0308};
		assertEquals(0, WcWidth.zeroWidthCharsCount(chars, -1, 2));
		assertEquals(0, WcWidth.zeroWidthCharsCount(chars, 5, 6));
	}

}
