package com.termux.terminal;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class TerminalEmulatorDecSetMappingTest extends TestCase {

	private static final int[] SUPPORTED_DECSET_NUMBERS = {1, 5, 6, 7, 25, 66, 69, 1000, 1002, 1004, 1006, 2004};

	public void testSupportedDecSetNumbersMapToDistinctPositiveBits() {
		Set<Integer> internalBits = new HashSet<>();
		for (int decsetNumber : SUPPORTED_DECSET_NUMBERS) {
			int internalBit = TerminalEmulator.mapDecSetBitToInternalBit(decsetNumber);
			assertTrue("DECSET " + decsetNumber + " must map to a positive internal bit but got " + internalBit, internalBit > 0);
			assertTrue("DECSET " + decsetNumber + " maps to a duplicate internal bit " + internalBit, internalBits.add(internalBit));
		}
		assertEquals(SUPPORTED_DECSET_NUMBERS.length, internalBits.size());
	}

	public void testUnsupportedDecSetNumbersReturnMinusOne() {
		int[] unsupported = {0, 2, 3, 4, 8, 9, 1001, 1003, 9999, -1};
		for (int decsetNumber : unsupported) {
			assertEquals("DECSET " + decsetNumber + " must be unsupported", -1, TerminalEmulator.mapDecSetBitToInternalBit(decsetNumber));
		}
	}

}
