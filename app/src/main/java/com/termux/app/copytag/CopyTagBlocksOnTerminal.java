package com.termux.app.copytag;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;

import java.util.ArrayList;
import java.util.List;

public final class CopyTagBlocksOnTerminal {

    private static final int MAX_ROWS_SCANNED_IN_EACH_DIRECTION = 400;

    private CopyTagBlocksOnTerminal() {
    }

    public static CopyTagBlocksOnScreen around(TerminalEmulator emulator, int row) {
        TerminalBuffer screen = emulator.getScreen();
        int topmostRow = Math.max(-screen.getActiveTranscriptRows(), row - MAX_ROWS_SCANNED_IN_EACH_DIRECTION);
        int bottommostRow = Math.min(emulator.mRows - 1, row + MAX_ROWS_SCANNED_IN_EACH_DIRECTION);

        List<ScreenRow> rows = new ArrayList<>();
        for (int scannedRow = topmostRow; scannedRow <= bottommostRow; scannedRow++) {
            String text = screen.getSelectedText(0, scannedRow, emulator.mColumns - 1, scannedRow, false, false);
            rows.add(new ScreenRow(text, screen.getLineWrap(scannedRow)));
        }
        return new CopyTagBlocksOnScreen(rows, topmostRow);
    }
}
