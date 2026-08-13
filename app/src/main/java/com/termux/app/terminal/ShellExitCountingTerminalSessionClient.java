package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.diagnostics.DiagnosticEventLogHolder;
import com.termux.app.diagnostics.DiagnosticEventType;
import com.termux.app.diagnostics.ShellExitStatusRecorderHolder;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;

public class ShellExitCountingTerminalSessionClient extends TermuxTerminalSessionClientBase {

    @Override
    public final void onSessionFinished(@NonNull TerminalSession finishedSession) {
        int shellExitStatus = finishedSession.getExitStatus();
        ShellExitStatusRecorderHolder.getInstance().recordShellExit(shellExitStatus);
        DiagnosticEventLogHolder.record(DiagnosticEventType.SESSION_EXITED,
            shellExitEventDetail(finishedSession.mSessionName, shellExitStatus));
        onShellProcessEnded(finishedSession);
    }

    @NonNull
    static String shellExitEventDetail(@Nullable String sessionName, int shellExitStatus) {
        return (sessionName == null ? "" : sessionName) + " exit=" + shellExitStatus;
    }

    protected void onShellProcessEnded(@NonNull TerminalSession finishedSession) {
    }
}
