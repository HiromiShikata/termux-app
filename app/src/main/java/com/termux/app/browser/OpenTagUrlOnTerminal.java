package com.termux.app.browser;

import com.termux.app.outputtag.OutputTagBlocksOnTerminal;
import com.termux.terminal.TerminalEmulator;

public final class OpenTagUrlOnTerminal {

    private static final String OPEN_TAG_NAME = "open";

    private OpenTagUrlOnTerminal() {
    }

    public static String urlCoveringRow(TerminalEmulator emulator, int row) {
        String tagContent = OutputTagBlocksOnTerminal.around(OPEN_TAG_NAME, emulator, row)
            .contentOfTheBlockCoveringRow(row);
        return OpenTagScanner.normalizeUrl(tagContent);
    }
}
