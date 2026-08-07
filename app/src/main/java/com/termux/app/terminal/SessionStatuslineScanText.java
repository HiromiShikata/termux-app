package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;

/**
 * The screen text the {@code call:}/{@code out:}/{@code reply:} statusline tokens are parsed out of.
 *
 * <p>{@link #of} is the whole main-buffer transcript followed by the visible screen, used where a
 * session's statusline may have to be found after a cold start. {@link #visibleScreenOf} is the
 * visible screen alone, used on the per-output path where the statusline is being re-rendered and is
 * therefore on screen: the tokens parsed out of it are the same ones the full text would yield,
 * because the visible screen is what the full text ends with, and a screen carrying no statusline
 * yields no token at all so the cheaper read can never store a value the full read would not.
 *
 * <p>{@link #of} reads the main buffer's transcript rather than the active one's, because the
 * alternate buffer a full-screen program holds keeps no scrollback and would hide a statusline that
 * scrolled off before that program started.
 */
public final class SessionStatuslineScanText {

    private SessionStatuslineScanText() {
    }

    @NonNull
    public static String of(@NonNull TerminalEmulator emulator, @NonNull TerminalBuffer screen) {
        return withTranscript(emulator, screen, emulator.getMainBufferTranscriptText());
    }

    @NonNull
    private static String withTranscript(@NonNull TerminalEmulator emulator,
                                         @NonNull TerminalBuffer screen,
                                         @NonNull String mainBufferTranscriptText) {
        StringBuilder builder = new StringBuilder(mainBufferTranscriptText);
        builder.append('\n').append(visibleScreenOf(emulator, screen));
        return builder.toString();
    }

    @NonNull
    public static String visibleScreenOf(@NonNull TerminalEmulator emulator,
                                         @NonNull TerminalBuffer screen) {
        int screenRows = emulator.mRows;
        int columns = emulator.mColumns;
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < screenRows; row++) {
            if (row > 0) {
                builder.append('\n');
            }
            builder.append(screen.getSelectedText(0, row, columns, row, false, false));
        }
        return builder.toString();
    }
}
