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
        // them on every text change fires each tag exactly once.
        scanOutputTags(changedSession);
    }

    @Override
    public void onGenuineOutput(@NonNull TerminalSession changedSession) {
        recordGenuineOutputActivity(changedSession);
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
        // A bell is delivered on every BEL byte, including bells echoed back from the user's own
        // keystrokes and repeated bells that carry no new process output. Record output activity
        // only when genuinely new process output accompanies it, using the same genuine-output gate
        // as onTextChanged, so out: is not pinned to "now" on every bell.
        recordGenuineOutputActivity(session);
    }

    private void recordGenuineOutputActivity(@NonNull TerminalSession session) {
        if (session.mSessionName == null) return;
        if (!mSessionOutputProgressTracker.hasNewOutput(
                session.mSessionName, session.getCommittedOutputLineCount())) {
            return;
        }
        mService.getSessionNewActivityStore().recordOutputActivity(session.mSessionName, System.currentTimeMillis());
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TermuxSession termuxSession = mService.getTermuxSessionForTerminalSession(terminalSession);
        if (termuxSession != null)
            termuxSession.getExecutionCommand().mPid = pid;
    }

}
