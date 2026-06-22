package com.termux.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.termux.app.TermuxService;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class TermuxTerminalSessionServiceClient extends TermuxTerminalSessionClientBase {

    private static final String LOG_TAG = "TermuxTerminalSessionServiceClient";

    private final TermuxService mService;

    public TermuxTerminalSessionServiceClient(TermuxService service) {
        this.mService = service;
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        recordOutputActivity(changedSession);
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
        recordOutputActivity(session);
    }

    @Override
    public void onMarkerNotification(@NonNull TerminalSession session) {
        recordExplicitCall(session);
    }

    @Override
    public void onUrgentNotification(@NonNull TerminalSession session) {
        recordExplicitCall(session);
    }

    private void recordOutputActivity(@NonNull TerminalSession session) {
        if (session.mSessionName == null) return;
        mService.getSessionNewActivityStore().recordOutputActivity(session.mSessionName, System.currentTimeMillis());
    }

    private void recordExplicitCall(@NonNull TerminalSession session) {
        if (session.mSessionName == null) return;
        mService.getSessionNewActivityStore().recordExplicitCall(session.mSessionName, System.currentTimeMillis());
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TermuxSession termuxSession = mService.getTermuxSessionForTerminalSession(terminalSession);
        if (termuxSession != null)
            termuxSession.getExecutionCommand().mPid = pid;
    }

}
