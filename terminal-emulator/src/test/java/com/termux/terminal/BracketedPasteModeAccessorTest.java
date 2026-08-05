package com.termux.terminal;

public class BracketedPasteModeAccessorTest extends TerminalTestCase {

	public void testBracketedPasteModeIsReportedToCallersOutsideTheEmulator() {
		withTerminalSized(3, 3);
		assertFalse("A terminal that has not enabled bracketed paste reports it disabled",
			mTerminal.isBracketedPasteMode());
		enterString("\033[?2004h");
		assertTrue("Enabling DECSET 2004 is reported to callers", mTerminal.isBracketedPasteMode());
		enterString("\033[?2004l");
		assertFalse("Disabling DECSET 2004 is reported to callers", mTerminal.isBracketedPasteMode());
	}
}
