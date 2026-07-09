package com.termux.terminal;

import junit.framework.TestCase;

public class TerminalBufferTest extends TestCase {

	private static final int COLUMNS = 5;
	private static final int SCREEN_ROWS = 3;
	private static final int TOTAL_ROWS = 8;

	private TerminalBuffer buffer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		buffer = new TerminalBuffer(COLUMNS, TOTAL_ROWS, SCREEN_ROWS);
	}

	private void writeRow(int row, String text) {
		for (int column = 0; column < text.length(); column++)
			buffer.setChar(column, row, text.charAt(column), TextStyle.NORMAL);
	}

	private String rowText(int externalRow) {
		return buffer.getSelectedText(0, externalRow, COLUMNS - 1, externalRow);
	}

	public void testFreshBufferIsBlank() {
		assertEquals(0, buffer.getActiveTranscriptRows());
		assertEquals(SCREEN_ROWS, buffer.getActiveRows());
		assertEquals("", buffer.getTranscriptText());
	}

	public void testSetCharStoresCodePointAndStyle() {
		long style = TextStyle.encode(3, 4, TextStyle.CHARACTER_ATTRIBUTE_BOLD);
		buffer.setChar(2, 1, 'Z', style);
		assertEquals(style, buffer.getStyleAt(1, 2));
		assertEquals("  Z", rowText(1));
	}

	public void testSetCharRejectsOutOfBounds() {
		try {
			buffer.setChar(COLUMNS, 0, 'a', TextStyle.NORMAL);
			fail("Expected IllegalArgumentException for column out of bounds");
		} catch (IllegalArgumentException expected) {
		}
		try {
			buffer.setChar(0, SCREEN_ROWS, 'a', TextStyle.NORMAL);
			fail("Expected IllegalArgumentException for row out of bounds");
		} catch (IllegalArgumentException expected) {
		}
		try {
			buffer.setChar(-1, 0, 'a', TextStyle.NORMAL);
			fail("Expected IllegalArgumentException for negative column");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testExternalToInternalRowMapping() {
		assertEquals(0, buffer.externalToInternalRow(0));
		assertEquals(SCREEN_ROWS - 1, buffer.externalToInternalRow(SCREEN_ROWS - 1));
	}

	public void testExternalToInternalRowRejectsOutOfRange() {
		try {
			buffer.externalToInternalRow(-1);
			fail("Expected IllegalArgumentException for row below transcript");
		} catch (IllegalArgumentException expected) {
		}
		try {
			buffer.externalToInternalRow(SCREEN_ROWS + 1);
			fail("Expected IllegalArgumentException for row above screen");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testLineWrapFlags() {
		assertFalse(buffer.getLineWrap(0));
		buffer.setLineWrap(0);
		assertTrue(buffer.getLineWrap(0));
		buffer.clearLineWrap(0);
		assertFalse(buffer.getLineWrap(0));
	}

	public void testScrollDownOneLineMovesContentIntoTranscript() {
		writeRow(0, "row0");
		writeRow(1, "row1");
		writeRow(2, "row2");

		buffer.scrollDownOneLine(0, SCREEN_ROWS, TextStyle.NORMAL);

		assertEquals(1, buffer.getActiveTranscriptRows());
		assertEquals(SCREEN_ROWS + 1, buffer.getActiveRows());
		assertEquals("row0", rowText(-1));
		assertEquals("row1", rowText(0));
		assertEquals("row2", rowText(1));
		assertEquals("", rowText(2));
	}

	public void testScrollDownAccumulatesTranscriptUpToCapacity() {
		int transcriptCapacity = TOTAL_ROWS - SCREEN_ROWS;
		for (int i = 0; i < transcriptCapacity + 3; i++) {
			writeRow(SCREEN_ROWS - 1, "L" + i);
			buffer.scrollDownOneLine(0, SCREEN_ROWS, TextStyle.NORMAL);
		}
		assertEquals(transcriptCapacity, buffer.getActiveTranscriptRows());
	}

	public void testScrollDownWithTopMarginKeepsTopRowFixed() {
		writeRow(0, "head");
		writeRow(1, "aaa");
		writeRow(2, "bbb");

		buffer.scrollDownOneLine(1, SCREEN_ROWS, TextStyle.NORMAL);

		assertEquals("head", rowText(0));
		assertEquals("bbb", rowText(1));
		assertEquals("", rowText(2));
	}

	public void testScrollDownRejectsInvalidMargins() {
		try {
			buffer.scrollDownOneLine(2, 1, TextStyle.NORMAL);
			fail("Expected IllegalArgumentException when topMargin exceeds bottomMargin");
		} catch (IllegalArgumentException expected) {
		}
		try {
			buffer.scrollDownOneLine(-1, SCREEN_ROWS, TextStyle.NORMAL);
			fail("Expected IllegalArgumentException for negative topMargin");
		} catch (IllegalArgumentException expected) {
		}
		try {
			buffer.scrollDownOneLine(0, SCREEN_ROWS + 1, TextStyle.NORMAL);
			fail("Expected IllegalArgumentException when bottomMargin exceeds screen rows");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testBlockSetClearsRegion() {
		writeRow(0, "abcde");
		buffer.blockSet(1, 0, 3, 1, 'X', TextStyle.NORMAL);
		assertEquals("aXXXe", rowText(0));
	}

	public void testBlockSetRejectsOutOfBounds() {
		try {
			buffer.blockSet(3, 0, 4, 1, 'X', TextStyle.NORMAL);
			fail("Expected IllegalArgumentException when block exceeds columns");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testBlockCopyMovesCharacters() {
		writeRow(0, "abcde");
		buffer.blockCopy(0, 0, 2, 1, 3, 0);
		assertEquals("abcab", rowText(0));
	}

	public void testBlockCopyRejectsOutOfBounds() {
		try {
			buffer.blockCopy(0, 0, COLUMNS + 1, 1, 0, 0);
			fail("Expected IllegalArgumentException when source width exceeds columns");
		} catch (IllegalArgumentException expected) {
		}
	}

	public void testGetSelectedTextAcrossRows() {
		writeRow(0, "hello");
		writeRow(1, "world");
		assertEquals("hello\nworld", buffer.getSelectedText(0, 0, COLUMNS - 1, 1));
	}

	public void testGetSelectedTextJoinsWrappedLines() {
		writeRow(0, "hello");
		writeRow(1, "world");
		buffer.setLineWrap(0);
		assertEquals("helloworld", buffer.getSelectedText(0, 0, COLUMNS - 1, 1, true));
		assertEquals("hello\nworld", buffer.getSelectedText(0, 0, COLUMNS - 1, 1, false));
	}

	public void testGetTranscriptTextReadsHistoryAndScreen() {
		writeRow(2, "one");
		buffer.scrollDownOneLine(0, SCREEN_ROWS, TextStyle.NORMAL);
		writeRow(2, "two");
		assertTrue(buffer.getTranscriptText().startsWith("one"));
		assertTrue(buffer.getTranscriptText().contains("two"));
	}

	public void testGetWordAtLocation() {
		writeRow(0, "ab cd");
		assertEquals("ab", buffer.getWordAtLocation(0, 0));
		assertEquals("cd", buffer.getWordAtLocation(3, 0));
		assertEquals("", buffer.getWordAtLocation(2, 0));
	}

	public void testGetWrappedLineAtLocationJoinsWrappedRows() {
		writeRow(0, "aaaaa");
		writeRow(1, "bbbbb");
		buffer.setLineWrap(0);
		WrappedLineLocation location = buffer.getWrappedLineAtLocation(2, 0);
		assertEquals("aaaaabbbbb", location.text);
		assertEquals(2, location.offset);
	}

	public void testGetWrappedLineAtLocationFromContinuationRow() {
		writeRow(0, "aaaaa");
		writeRow(1, "bbbbb");
		buffer.setLineWrap(0);
		WrappedLineLocation location = buffer.getWrappedLineAtLocation(3, 1);
		assertEquals("aaaaabbbbb", location.text);
		assertEquals(8, location.offset);
	}

	public void testGetWrappedLineAtLocationNonWrappedRowIsSingleRow() {
		writeRow(0, "aa");
		writeRow(1, "bbbbb");
		WrappedLineLocation location = buffer.getWrappedLineAtLocation(1, 0);
		assertEquals("aa", location.text);
		assertEquals(1, location.offset);
	}

	public void testHyperlinkUriStorageAndLookup() {
		buffer.setChar(1, 0, 'L', TextStyle.NORMAL, "https://example.com");
		assertEquals("https://example.com", buffer.getHyperlinkUri(0, 1));
		assertNull(buffer.getHyperlinkUri(0, 0));
		assertNull(buffer.getHyperlinkUri(0, -1));
		assertNull(buffer.getHyperlinkUri(0, COLUMNS));
		assertNull(buffer.getHyperlinkUri(SCREEN_ROWS, 1));
	}

	public void testSetOrClearEffectSetsEffectBits() {
		buffer.setChar(0, 0, 'a', TextStyle.encode(1, 2, 0));
		buffer.setOrClearEffect(TextStyle.CHARACTER_ATTRIBUTE_BOLD, true, false, true, 0, COLUMNS, 0, 0, 1, 1);
		long style = buffer.getStyleAt(0, 0);
		assertTrue((TextStyle.decodeEffect(style) & TextStyle.CHARACTER_ATTRIBUTE_BOLD) != 0);
		assertEquals(1, TextStyle.decodeForeColor(style));
		assertEquals(2, TextStyle.decodeBackColor(style));
	}

	public void testSetOrClearEffectClearsAndReverses() {
		long bold = TextStyle.encode(1, 2, TextStyle.CHARACTER_ATTRIBUTE_BOLD);
		buffer.setChar(0, 0, 'a', bold);
		buffer.setOrClearEffect(TextStyle.CHARACTER_ATTRIBUTE_BOLD, false, false, true, 0, COLUMNS, 0, 0, 1, 1);
		assertEquals(0, TextStyle.decodeEffect(buffer.getStyleAt(0, 0)) & TextStyle.CHARACTER_ATTRIBUTE_BOLD);

		buffer.setChar(0, 0, 'a', bold);
		buffer.setOrClearEffect(TextStyle.CHARACTER_ATTRIBUTE_BOLD, true, true, true, 0, COLUMNS, 0, 0, 1, 1);
		assertEquals(0, TextStyle.decodeEffect(buffer.getStyleAt(0, 0)) & TextStyle.CHARACTER_ATTRIBUTE_BOLD);
	}

	public void testClearTranscriptRemovesHistory() {
		writeRow(0, "head");
		buffer.scrollDownOneLine(0, SCREEN_ROWS, TextStyle.NORMAL);
		assertEquals(1, buffer.getActiveTranscriptRows());
		assertEquals("head", rowText(-1));
		buffer.clearTranscript();
		assertEquals(0, buffer.getActiveTranscriptRows());
		assertEquals("", buffer.getTranscriptText());
	}

	public void testResizeShrinkingRowsScrollsTopIntoTranscript() {
		writeRow(0, "aaa");
		writeRow(1, "bbb");
		writeRow(2, "ccc");
		int[] cursor = {0, 2};
		buffer.resize(COLUMNS, 2, TOTAL_ROWS, cursor, TextStyle.NORMAL, false);
		assertEquals(2, buffer.mScreenRows);
		assertEquals(1, buffer.getActiveTranscriptRows());
		assertEquals("aaa", rowText(-1));
		assertEquals("bbb", rowText(0));
		assertEquals("ccc", rowText(1));
	}

	public void testResizeExpandingRowsRevealsTranscript() {
		writeRow(0, "top");
		buffer.scrollDownOneLine(0, SCREEN_ROWS, TextStyle.NORMAL);
		assertEquals(1, buffer.getActiveTranscriptRows());
		int[] cursor = {0, 0};
		buffer.resize(COLUMNS, SCREEN_ROWS + 1, TOTAL_ROWS, cursor, TextStyle.NORMAL, false);
		assertEquals(SCREEN_ROWS + 1, buffer.mScreenRows);
		assertEquals(0, buffer.getActiveTranscriptRows());
		assertEquals("top", rowText(0));
	}

	public void testResizeChangingColumnsReflowsText() {
		writeRow(0, "abcde");
		int[] cursor = {5, 0};
		buffer.resize(3, SCREEN_ROWS, TOTAL_ROWS, cursor, TextStyle.NORMAL, false);
		assertEquals(3, buffer.mColumns);
		assertEquals("abcde", buffer.getTranscriptTextWithFullLinesJoined());
	}

	public void testAllocateFullLineIfNecessary() {
		int internal = buffer.externalToInternalRow(0);
		TerminalRow first = buffer.allocateFullLineIfNecessary(internal);
		assertNotNull(first);
		assertSame(first, buffer.allocateFullLineIfNecessary(internal));
	}
}
