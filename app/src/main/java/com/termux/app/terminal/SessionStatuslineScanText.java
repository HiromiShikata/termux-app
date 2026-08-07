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
 * <p>{@link #reusingActiveBufferTranscript} yields the same text as {@link #of} for a caller that has
 * already materialised the active buffer's transcript for another scan: while the main buffer is the
 * active one those two transcripts are the same object's content, so the caller's copy is reused
 * rather than the whole scrollback being walked a second time, and while a full-screen program holds
 * the alternate buffer the main-buffer transcript is read because the alternate buffer keeps no
 * scrollback and would hide a statusline that scrolled off before the program started.
 */
public final class SessionStatuslineScanText {

    private SessionStatuslineScanText() {
    }

    @NonNull
    public static String of(@NonNull TerminalEmulator emulator, @NonNull TerminalBuffer screen) {
        return withTranscript(emulator, screen, emulator.getMainBufferTranscriptText());
    }

    @NonNull
    public static String reusingActiveBufferTranscript(@NonNull TerminalEmulator emulator,
                                                       @NonNull TerminalBuffer screen,
                                                       @NonNull String activeBufferTranscriptText) {
        return withTranscript(emulator, screen, emulator.isAlternateBufferActive()
            ? emulator.getMainBufferTranscriptText()
            : activeBufferTranscriptText);
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
