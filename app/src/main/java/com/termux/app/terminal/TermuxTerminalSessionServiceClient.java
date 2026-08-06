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

import java.util.TimeZone;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class TermuxTerminalSessionServiceClient extends TermuxTerminalSessionClientBase {

    private static final String LOG_TAG = "TermuxTerminalSessionServiceClient";

    private final TermuxService mService;

    private final SessionOutputProgressTracker mSessionOutputProgressTracker = new SessionOutputProgressTracker();

    private final SessionStatuslineTimesRecorder mSessionStatuslineTimesRecorder =
        new SessionStatuslineTimesRecorder();

    public TermuxTerminalSessionServiceClient(TermuxService service) {
        this.mService = service;
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        if (changedSession.mSessionName == null) return;

        // The statusline call:/out:/reply: tokens are recorded for every session that produces
        // output while the activity is unbound (app backgrounded), exactly as the activity client
        // records them while bound. They are the only signal an owner call whose tag never appears
        // literally in the transcript produces, so a session called while backgrounded records its
        // red dot when the call is rendered rather than waiting for a later rescan.
        recordStatuslineTimes(changedSession);

        // Scan the explicit-call and app-update output tags for every session, even while the
        // activity is unbound (app backgrounded). This records an explicit call into the shared
        // activity store so the session list shows the red dot whenever the list is next viewed,
        // without the owner having to open the producing session. The open-URL tag stays
        // activity-only (handled by the activity client for the current session) and is not
        // scanned here. The controllers keep one scanner per session and deduplicate, so calling
        // them on every text change fires each tag exactly once.
        scanOutputTags(changedSession);
    }

    private void recordStatuslineTimes(@NonNull TerminalSession session) {
        SessionNewActivityStore store = mService.getSessionNewActivityStore();
        if (store == null) return;
        mSessionStatuslineTimesRecorder.recordFromVisibleScreen(store, session,
            System.currentTimeMillis(), TimeZone.getDefault());
    }

    @Override
    public void onGenuineOutput(@NonNull TerminalSession changedSession) {
        // onGenuineOutput fires only on genuine process output past the stripped keystroke echo,
        // regardless of whether the main or the alternate screen buffer is active, and never from
        // scrolling, viewport changes, or redraws. Recording output activity directly on every such
        // event keeps a full-screen alternate-buffer program's out: time advancing while it emits
        // output, without keying on committed scrollback growth (which never advances in the
        // alternate buffer).
        recordOutputActivity(changedSession);
    }

    private void scanOutputTags(@NonNull TerminalSession session) {
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        TerminalBuffer screen = emulator.getScreen();
        if (screen == null) return;

        // The tag scan stays unconditional here, unlike the activity client which gates it on the
        // statusline-pending state, so a session that calls the user while the app is backgrounded
        // records its red dot from the tag as well as from the statusline times recorded above.
        new BackgroundOutputTagScanner(
            mService.getCallToUserTagController(),
            mService.getUpdateTagUpdateController())
            .scan(session.mHandle, screen.getTranscriptText(), true);
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
