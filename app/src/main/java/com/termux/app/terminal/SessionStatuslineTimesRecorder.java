package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import java.util.TimeZone;

/**
 * Records a session's statusline-derived call/out/reply times from the statusline currently on its
 * screen. Both session clients use it on every output, so the owner-call relation the red dot is
 * derived from ({@code call:} newer than the genuine reply) is observed at the moment the call is
 * rendered, whether the activity is bound or the app is in the background.
 */
public final class SessionStatuslineTimesRecorder {

    private final SessionStatuslineReloadScanner mScanner = new SessionStatuslineReloadScanner();

    public void recordFromVisibleScreen(@NonNull SessionNewActivityStore store,
                                        @NonNull TerminalSession session,
                                        long nowMillis,
                                        @NonNull TimeZone timeZone) {
        String sessionName = session.mSessionName;
        if (sessionName == null) return;
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        TerminalBuffer screen = emulator.getScreen();
        if (screen == null) return;
        mScanner.repopulateFromCurrentStatusline(store, sessionName,
            SessionStatuslineScanText.visibleScreenOf(emulator, screen), nowMillis, timeZone);
    }
}
