package com.termux.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class TermuxTerminalSessionServiceClient extends TermuxTerminalSessionClientBase {

    private static final String LOG_TAG = "TermuxTerminalSessionServiceClient";

    private final TermuxService mService;

    private final SessionOutputProgressTracker mSessionOutputProgressTracker = new SessionOutputProgressTracker();

    public TermuxTerminalSessionServiceClient(TermuxService service) {
        this.mService = service;
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        if (changedSession.mSessionName == null) return;

        // Scan the explicit-call and app-update output tags for every session, even while the
        // activity is unbound (app backgrounded). This records an explicit call into the shared
        // activity store so the session list shows the red dot whenever the list is next viewed,
        // without the owner having to open the producing session. The open-URL tag stays
        // activity-only (handled by the activity client for the current session) and is not
        // scanned here. The controllers keep one scanner per session and deduplicate, so calling
        // them on every text change fires each tag exactly once. They are scanned before the
        // output-progress early return below so a tag is never skipped on a redraw-only update.
        scanOutputTags(changedSession);

        if (!mSessionOutputProgressTracker.hasNewOutput(
                changedSession.mSessionName, changedSession.getGenuineOutputVersion())) {
            return;
        }
        recordOutputActivity(changedSession);
    }

    private void scanOutputTags(@NonNull TerminalSession session) {
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        TerminalBuffer screen = emulator.getScreen();
        if (screen == null) return;

        new BackgroundOutputTagScanner(
            mService.getCallToUserTagController(),
            mService.getUpdateTagUpdateController())
            .scan(session.mHandle, screen.getTranscriptText());
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
        recordOutputActivity(session);
    }

    private void recordOutputActivity(@NonNull TerminalSession session) {
        if (session.mSessionName == null) return;
        mService.getSessionNewActivityStore().recordOutputActivity(session.mSessionName, System.currentTimeMillis());
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TermuxSession termuxSession = mService.getTermuxSessionForTerminalSession(terminalSession);
        if (termuxSession != null)
            termuxSession.getExecutionCommand().mPid = pid;
    }

}
