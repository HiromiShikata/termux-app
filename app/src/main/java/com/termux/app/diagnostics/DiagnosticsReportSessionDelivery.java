package com.termux.app.diagnostics;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.io.TerminalEnterKeyEncoder;
import com.termux.terminal.ShellInputDeliveryRecord;
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
        String reportText = mReportBuilder.buildForDeliveryIntoASession(report);

        long pasteStartMillis = SystemClock.elapsedRealtime();
        emulator.paste(reportText);
        long pasteMillis = SystemClock.elapsedRealtime() - pasteStartMillis;

        boolean inputReachedTheProgramAfterThePaste = session.inputReachesTheProgramReadingTheTerminal();

        ShellInputDeliveryRecord deliveryRecord = session.getShellInputDeliveryRecord();
        long bytesAcceptedBeforeTheEnter = deliveryRecord.getBytesAcceptedForDelivery();
        session.write(TerminalEnterKeyEncoder.enterSequence(
            emulator.isCursorKeysApplicationMode(), emulator.isKeypadApplicationMode()));
        long bytesAcceptedAfterTheEnter = deliveryRecord.getBytesAcceptedForDelivery();

        DiagnosticsReportDeliveryRecorderHolder.getInstance().recordDelivery(
            DiagnosticsReportDelivery.of(sessionNameOf(session), reportText.length(), pasteMillis,
                bytesAcceptedBeforeTheEnter, bytesAcceptedAfterTheEnter,
                inputReachedTheProgramAfterThePaste));
        return true;
    }

    @NonNull
    private static String sessionNameOf(@NonNull TerminalSession session) {
        return session.mSessionName == null ? "" : session.mSessionName;
    }
}
