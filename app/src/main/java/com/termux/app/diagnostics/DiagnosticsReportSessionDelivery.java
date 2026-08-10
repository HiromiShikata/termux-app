package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.io.TerminalEnterKeyEncoder;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

public final class DiagnosticsReportSessionDelivery {

    @NonNull
    private final DiagnosticsReportCollector mCollector;

    @NonNull
    private final DiagnosticsReportBuilder mReportBuilder;

    public DiagnosticsReportSessionDelivery() {
        this(new DiagnosticsReportCollector(), new DiagnosticsReportBuilder());
    }

    public DiagnosticsReportSessionDelivery(@NonNull DiagnosticsReportCollector collector,
                                            @NonNull DiagnosticsReportBuilder reportBuilder) {
        mCollector = collector;
        mReportBuilder = reportBuilder;
    }

    public boolean deliverTo(@NonNull TermuxActivity activity, @NonNull TerminalSession session) {
        if (!session.isRunning()) return false;
        if (!session.inputReachesTheProgramReadingTheTerminal()) return false;

        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return false;

        DiagnosticsReport report = mCollector.collect(activity, System.currentTimeMillis());
        emulator.paste(mReportBuilder.build(report));
        session.write(TerminalEnterKeyEncoder.enterSequence(
            emulator.isCursorKeysApplicationMode(), emulator.isKeypadApplicationMode()));
        return true;
    }
}
