package com.termux.app.copytag;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;

public final class CopyTagBlocksOnTerminal {

    private static final int MAX_ROWS_SCANNED_IN_EACH_DIRECTION = 400;

    private CopyTagBlocksOnTerminal() {
    }

    public static CopyTagBlocksOnScreen around(TerminalEmulator emulator, int row) {
        TerminalBuffer screen = emulator.getScreen();
        int lastColumn = emulator.mColumns - 1;
        int topmostRow = Math.max(-screen.getActiveTranscriptRows(), row - MAX_ROWS_SCANNED_IN_EACH_DIRECTION);
        int bottommostRow = Math.min(emulator.mRows - 1, row + MAX_ROWS_SCANNED_IN_EACH_DIRECTION);

        ScreenRows rows = rowNumber -> {
            if (rowNumber < topmostRow || rowNumber > bottommostRow) return null;
            String text = screen.getSelectedText(0, rowNumber, lastColumn, rowNumber, false, false);
            return new ScreenRow(text, screen.getLineWrap(rowNumber));
        };
        return new CopyTagBlocksOnScreen(rows, topmostRow, bottommostRow);
    }
}
