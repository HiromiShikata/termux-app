package com.termux.terminal;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A terminal session, consisting of a process coupled to a terminal interface.
 * <p>
 * The subprocess will be executed by the constructor, and when the size is made known by a call to
 * {@link #updateSize(int, int, int, int)} terminal emulation will begin and threads will be spawned to handle the subprocess I/O.
 * All terminal emulation and callback methods will be performed on the main thread.
 * <p>
 * The child process may be exited forcefully by using the {@link #finishIfRunning()} method.
 * <p>
 * NOTE: The terminal session may outlive the EmulatorView, so be careful with callbacks!
 */
public final class TerminalSession extends TerminalOutput {

    private static final int MSG_NEW_INPUT = 1;
    private static final int MSG_PROCESS_EXITED = 4;

    public static final int NO_SHELL_PROCESS_PID = -1;

    static final int PROCESS_TO_TERMINAL_IO_QUEUE_CAPACITY_BYTES = 64 * 1024;

    static final int TERMINAL_TO_PROCESS_IO_QUEUE_CAPACITY_BYTES = 4096;

    public final String mHandle = UUID.randomUUID().toString();

    TerminalEmulator mEmulator;

    private volatile boolean mRuntimeResourcesReleased;

    private volatile int mSigkilledShellProcessGroupTarget = NO_SHELL_PROCESS_GROUP_TARGET;

    /**
     * A queue written to from a separate thread when the process outputs, and read by main thread to process by
     * terminal emulator.
     */
    ByteQueue mProcessToTerminalIOQueue = new ByteQueue(PROCESS_TO_TERMINAL_IO_QUEUE_CAPACITY_BYTES);
    /**
     * A queue written to from the main thread due to user interaction, and read by another thread which forwards by
     * writing to the {@link #mTerminalFileDescriptor}.
     */
    ByteQueue mTerminalToProcessIOQueue = new ByteQueue(TERMINAL_TO_PROCESS_IO_QUEUE_CAPACITY_BYTES);
    /** Buffer to write translate code points into utf8 before writing to mTerminalToProcessIOQueue */
    private final byte[] mUtf8InputBuffer = new byte[5];

    /** Callback which gets notified when a session finishes or changes title. */
    TerminalSessionClient mClient;

    int mShellPid = NO_SHELL_PROCESS_PID;

    /** The exit status of the shell process. Only valid if ${@link #mShellPid} is -1. */
    int mShellExitStatus;

    /**
     * The file descriptor referencing the master half of a pseudo-terminal pair, resulting from calling
     * {@link JNI#createSubprocess(String, String, String[], String[], int[], int, int, int, int)}.
     */
    private int mTerminalFileDescriptor;

    int mShellProcessGeneration;

    /** Set by the application for user identification of session, not by terminal. */
    public String mSessionName;

    final Handler mMainThreadHandler = new MainThreadHandler();

    private final String mShellPath;
    private final String mCwd;
    private final String[] mArgs;
    private final String[] mEnv;
    private final Integer mTranscriptRows;


    private long mTotalBytesProcessed = 0L;

    private final TerminalInputEchoFilter mInputEchoFilter = new TerminalInputEchoFilter();

    private static final String LOG_TAG = "TerminalSession";

    public TerminalSession(String shellPath, String cwd, String[] args, String[] env, Integer transcriptRows, TerminalSessionClient client) {
        this.mShellPath = shellPath;
        this.mCwd = cwd;
        this.mArgs = args;
        this.mEnv = env;
        this.mTranscriptRows = transcriptRows;
        this.mClient = client;
    }

    /**
     * @param client The {@link TerminalSessionClient} interface implementation to allow
     *               for communication between {@link TerminalSession} and its client.
     */
    public void updateTerminalSessionClient(TerminalSessionClient client) {
        mClient = client;

        if (mEmulator != null)
            mEmulator.updateTerminalSessionClient(client);
    }

    /** Inform the attached pty of the new size and reflow or initialize the emulator. */
    public void updateSize(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        if (mRuntimeResourcesReleased) {
            return;
        }
        if (mEmulator == null) {
            initializeEmulator(columns, rows, cellWidthPixels, cellHeightPixels);
        } else {
            JNI.setPtyWindowSize(mTerminalFileDescriptor, rows, columns, cellWidthPixels, cellHeightPixels);
            mEmulator.resize(columns, rows, cellWidthPixels, cellHeightPixels);
        }
    }

    public void forceRemoteRepaint() {
        if (!isRunning() || mEmulator == null) return;
        int columns = mEmulator.mColumns;
        int rows = mEmulator.mRows;
        RemoteRepaintWindowSizeNudge nudge = RemoteRepaintWindowSizeNudge.forCurrentSize(columns, rows);
        if (!nudge.shouldNudge()) return;
        int cellWidthPixels = mEmulator.getCellWidthPixels();
        int cellHeightPixels = mEmulator.getCellHeightPixels();
        JNI.setPtyWindowSize(mTerminalFileDescriptor, nudge.getNudgedRows(), columns, cellWidthPixels, cellHeightPixels);
        JNI.setPtyWindowSize(mTerminalFileDescriptor, nudge.getRestoredRows(), columns, cellWidthPixels, cellHeightPixels);
    }

    /** The terminal title as set through escape sequences or null if none set. */
    public String getTitle() {
        return (mEmulator == null) ? null : mEmulator.getTitle();
    }

    /**
     * Set the terminal emulator's window size and start terminal emulation.
     *
     * @param columns The number of columns in the terminal window.
     * @param rows    The number of rows in the terminal window.
     */
    public void initializeEmulator(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        mEmulator = new TerminalEmulator(this, columns, rows, cellWidthPixels, cellHeightPixels, mTranscriptRows, mClient);
        replaceEachIOQueueAPreviousShellProcessClosed();

        final ByteQueue processToTerminalIOQueue = mProcessToTerminalIOQueue;
        final ByteQueue terminalToProcessIOQueue = mTerminalToProcessIOQueue;
        final int shellProcessGeneration = mShellProcessGeneration;

        int[] processId = new int[1];
        final int terminalFileDescriptor = JNI.createSubprocess(mShellPath, mCwd, mArgs, mEnv, processId, rows, columns, cellWidthPixels, cellHeightPixels);
        mTerminalFileDescriptor = terminalFileDescriptor;
        mShellPid = processId[0];
        mClient.setTerminalShellPid(this, mShellPid);

        final FileDescriptor terminalFileDescriptorWrapped = wrapFileDescriptor(terminalFileDescriptor, mClient);

        new Thread("TermSessionInputReader[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                try (InputStream termIn = new FileInputStream(terminalFileDescriptorWrapped)) {
                    final byte[] buffer = new byte[4096];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1) return;
                        if (!processToTerminalIOQueue.write(buffer, 0, read)) return;
                        mMainThreadHandler.sendEmptyMessage(MSG_NEW_INPUT);
                    }
                } catch (Exception e) {
                    // Ignore, just shutting down.
                }
            }
        }.start();

        new Thread("TermSessionOutputWriter[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                final byte[] buffer = new byte[4096];
                try (FileOutputStream termOut = new FileOutputStream(terminalFileDescriptorWrapped)) {
                    while (true) {
                        int bytesToWrite = terminalToProcessIOQueue.read(buffer, true);
                        if (bytesToWrite == -1) return;
                        termOut.write(buffer, 0, bytesToWrite);
                    }
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }.start();

        new Thread("TermSessionWaiter[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                int processExitCode = JNI.waitFor(mShellPid);
                mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(MSG_PROCESS_EXITED,
                    shellProcessGeneration, terminalFileDescriptor, processExitCode));
            }
        }.start();

    }

    /** Write data to the shell process. */
    @Override
    public void write(byte[] data, int offset, int count) {
        if (mShellPid > 0 && inputReachesTheProgramReadingTheTerminal()) {
            mInputEchoFilter.recordUserInput(data, offset, count);
            mTerminalToProcessIOQueue.write(data, offset, count);
        }
    }

    /**
     * Whether input written now is read by the program the session was started for, rather than being held by the line
     * discipline until some later program reads it as stale input.
     */
    public boolean inputReachesTheProgramReadingTheTerminal() {
        if (mRuntimeResourcesReleased || mShellPid <= 0) return false;
        return TerminalInputDelivery.reachesTheProgramReadingTheTerminal(
            RemoteShellClientCommand.isRunBy(mShellPath, mArgs),
            () -> JNI.isPtyInCanonicalMode(mTerminalFileDescriptor));
    }

    /** Write the Unicode code point to the terminal encoded in UTF-8. */
    public void writeCodePoint(boolean prependEscape, int codePoint) {
        if (codePoint > 1114111 || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            // 1114111 (= 2**16 + 1024**2 - 1) is the highest code point, [0xD800,0xDFFF] is the surrogate range.
            throw new IllegalArgumentException("Invalid code point: " + codePoint);
        }

        int bufferPosition = 0;
        if (prependEscape) mUtf8InputBuffer[bufferPosition++] = 27;

        if (codePoint <= /* 7 bits */0b1111111) {
            mUtf8InputBuffer[bufferPosition++] = (byte) codePoint;
        } else if (codePoint <= /* 11 bits */0b11111111111) {
            /* 110xxxxx leading byte with leading 5 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b11000000 | (codePoint >> 6));
            /* 10xxxxxx continuation byte with following 6 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | (codePoint & 0b111111));
        } else if (codePoint <= /* 16 bits */0b1111111111111111) {
            /* 1110xxxx leading byte with leading 4 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b11100000 | (codePoint >> 12));
            /* 10xxxxxx continuation byte with following 6 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | ((codePoint >> 6) & 0b111111));
            /* 10xxxxxx continuation byte with following 6 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | (codePoint & 0b111111));
        } else { /* We have checked codePoint <= 1114111 above, so we have max 21 bits = 0b111111111111111111111 */
            /* 11110xxx leading byte with leading 3 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b11110000 | (codePoint >> 18));
            /* 10xxxxxx continuation byte with following 6 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | ((codePoint >> 12) & 0b111111));
            /* 10xxxxxx continuation byte with following 6 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | ((codePoint >> 6) & 0b111111));
            /* 10xxxxxx continuation byte with following 6 bits */
            mUtf8InputBuffer[bufferPosition++] = (byte) (0b10000000 | (codePoint & 0b111111));
        }
        write(mUtf8InputBuffer, 0, bufferPosition);
    }

    public TerminalEmulator getEmulator() {
        return mEmulator;
    }

    public long getNeverResetScrolledLineCount() {
        return mEmulator == null ? 0L : mEmulator.getNeverResetScrolledLineCount();
    }

    public long getCommittedOutputLineCount() {
        return mEmulator == null ? 0L : mEmulator.getCommittedOutputLineCount();
    }

    public long getTotalBytesProcessed() { return mTotalBytesProcessed; }

    public long getVisibleContentVersion() {
        return mEmulator == null ? 0L : mEmulator.getVisibleContentVersion();
    }

    public long getScreenContentVersion() {
        return mEmulator == null ? 0L : mEmulator.getScreenContentVersion();
    }

    public long getRealOutputVersion() {
        return mEmulator == null ? 0L : mEmulator.getRealOutputVersion();
    }

    /** Notify the {@link #mClient} that the screen has changed. */
    protected void notifyScreenUpdate() {
        mClient.onTextChanged(this);
    }

    /**
     * Notify the {@link #mClient} that the running process genuinely emitted output. This fires only
     * from the pseudo-teletype input path after {@link TerminalEmulator#appendGenuineOutput} and only
     * when the received bytes contained genuine process output past the stripped keystroke echo. It
     * never fires from scrolling, viewport changes, redraws, or pure keystroke echoes, so
     * output-activity recording cannot be triggered by anything other than real process output. It
     * fires regardless of whether the main or the alternate screen buffer is active, so a full-screen
     * alternate-buffer program emitting output is registered as genuine output.
     */
    protected void notifyGenuineOutput() {
        mClient.onGenuineOutput(this);
    }

    /** Reset state for terminal emulator state. */
    public void reset() {
        if (mRuntimeResourcesReleased || mEmulator == null) {
            return;
        }
        mEmulator.reset();
        notifyScreenUpdate();
    }

    static final int NO_SHELL_PROCESS_GROUP_TARGET = 0;

    /**
     * The {@code kill(2)} target that terminates the started shell's process group, or
     * {@link #NO_SHELL_PROCESS_GROUP_TARGET} when no started shell owns one. Only a real shell pid
     * greater than 1 owns a killable process group: passing {@code kill(2)} a target of 0 broadcasts
     * SIGKILL to this app's own process group and a target of 1 addresses init.
     */
    static int shellProcessGroupKillTarget(int shellPid) {
        if (shellPid > 1) {
            return -shellPid;
        }
        return NO_SHELL_PROCESS_GROUP_TARGET;
    }

    static boolean shouldSendSigkillToProcessGroup(boolean running, int shellPid) {
        return running && shellProcessGroupKillTarget(shellPid) != NO_SHELL_PROCESS_GROUP_TARGET;
    }

    /** Finish this terminal session by sending SIGKILL to the shell process group. */
    public void finishIfRunning() {
        if (!shouldSendSigkillToProcessGroup(isRunning(), mShellPid)) {
            return;
        }
        int shellProcessGroupTarget = shellProcessGroupKillTarget(mShellPid);
        try {
            Os.kill(shellProcessGroupTarget, OsConstants.SIGKILL);
            mSigkilledShellProcessGroupTarget = shellProcessGroupTarget;
        } catch (ErrnoException e) {
            Logger.logWarn(mClient, LOG_TAG, "Failed sending SIGKILL to process group: " + e.getMessage());
        }
    }

    /** Cleanup resources when the process exits. */
    void cleanupResources(int exitStatus) {
        synchronized (this) {
            mShellPid = NO_SHELL_PROCESS_PID;
            mShellExitStatus = exitStatus;
        }

        // Stop the reader and writer threads, and close the I/O streams
        closeShellStreams();
    }

    static final int NO_TERMINAL_FILE_DESCRIPTOR = 0;

    private void closeShellStreams() {
        mTerminalToProcessIOQueue.close();
        mProcessToTerminalIOQueue.close();
        if (mTerminalFileDescriptor == NO_TERMINAL_FILE_DESCRIPTOR) {
            return;
        }
        JNI.close(mTerminalFileDescriptor);
        mTerminalFileDescriptor = NO_TERMINAL_FILE_DESCRIPTOR;
    }

    public void releaseRuntimeResources() {
        mRuntimeResourcesReleased = true;
        releaseRuntimeResourcesKeepingTheRowReopenable();
    }

    public void releaseRuntimeResourcesKeepingTheRowReopenable() {
        finishIfRunning();
        closeShellStreams();
        supersedeTheShellProcessInstanceTheSessionOwns();
        disownTheShellProcessIdentifier();
        mMainThreadHandler.removeCallbacksAndMessages(null);
        mEmulator = null;
    }

    private void supersedeTheShellProcessInstanceTheSessionOwns() {
        mShellProcessGeneration++;
    }

    private void replaceEachIOQueueAPreviousShellProcessClosed() {
        if (!mTerminalToProcessIOQueue.isOpen()) {
            mTerminalToProcessIOQueue = new ByteQueue(TERMINAL_TO_PROCESS_IO_QUEUE_CAPACITY_BYTES);
        }
        if (!mProcessToTerminalIOQueue.isOpen()) {
            mProcessToTerminalIOQueue = new ByteQueue(PROCESS_TO_TERMINAL_IO_QUEUE_CAPACITY_BYTES);
        }
    }

    private void disownTheShellProcessIdentifier() {
        synchronized (this) {
            mShellPid = NO_SHELL_PROCESS_PID;
        }
    }

    @Override
    public void titleChanged(String oldTitle, String newTitle) {
        mClient.onTitleChanged(this);
    }

    public synchronized boolean isRunning() {
        return mShellPid != NO_SHELL_PROCESS_PID;
    }

    /** Only valid if not {@link #isRunning()}. */
    public synchronized int getExitStatus() {
        return mShellExitStatus;
    }

    @Override
    public void onCopyTextToClipboard(String text) {
        mClient.onCopyTextToClipboard(this, text);
    }

    @Override
    public void onPasteTextFromClipboard() {
        mClient.onPasteTextFromClipboard(this);
    }

    @Override
    public void onBell() {
        mClient.onBell(this);
    }

    @Override
    public void onSpeakNotification(String text) {
        mClient.onSpeakNotification(this, text);
    }

    @Override
    public void onColorsChanged() {
        mClient.onColorsChanged(this);
    }

    public int getPid() {
        return mShellPid;
    }

    /** Returns the shell's working directory or null if it was unavailable. */
    public String getCwd() {
        if (mShellPid < 1) {
            return null;
        }
        try {
            final String cwdSymlink = String.format("/proc/%s/cwd/", mShellPid);
            String outputPath = new File(cwdSymlink).getCanonicalPath();
            String outputPathWithTrailingSlash = outputPath;
            if (!outputPath.endsWith("/")) {
                outputPathWithTrailingSlash += '/';
            }
            if (!cwdSymlink.equals(outputPathWithTrailingSlash)) {
                return outputPath;
            }
        } catch (IOException | SecurityException e) {
            Logger.logStackTraceWithMessage(mClient, LOG_TAG, "Error getting current directory", e);
        }
        return null;
    }

    private static FileDescriptor wrapFileDescriptor(int fileDescriptor, TerminalSessionClient client) {
        FileDescriptor result = new FileDescriptor();
        try {
            Field descriptorField;
            try {
                descriptorField = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                // For desktop java:
                descriptorField = FileDescriptor.class.getDeclaredField("fd");
            }
            descriptorField.setAccessible(true);
            descriptorField.set(result, fileDescriptor);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            Logger.logStackTraceWithMessage(client, LOG_TAG, "Error accessing FileDescriptor#descriptor private field", e);
            System.exit(1);
        }
        return result;
    }

    static boolean exitReportBelongsToASupersededShellProcess(int reportingShellProcessGeneration,
                                                              int ownedShellProcessGeneration) {
        return reportingShellProcessGeneration != ownedShellProcessGeneration;
    }

    @SuppressLint("HandlerLeak")
    class MainThreadHandler extends Handler {

        final byte[] mReceiveBuffer = new byte[64 * 1024];

        @Override
        public void handleMessage(Message msg) {
            renderPendingShellOutput();

            if (msg.what != MSG_PROCESS_EXITED) return;

            if (exitReportBelongsToASupersededShellProcess(msg.arg1, mShellProcessGeneration)) return;

            int exitCode = (Integer) msg.obj;
            cleanupResources(exitCode);
            if (mEmulator != null) renderShellCompletionNotice(exitCode);
            mClient.onSessionFinished(TerminalSession.this);
        }

        private void renderPendingShellOutput() {
            int bytesRead = mProcessToTerminalIOQueue.read(mReceiveBuffer, false);
            if (bytesRead <= 0) return;
            if (mEmulator == null) {
                Logger.logDebug(mClient, LOG_TAG, "Discarded " + bytesRead + " bytes of output of session \""
                    + mSessionName + "\" because its terminal emulator was released");
                return;
            }
            mTotalBytesProcessed += bytesRead;
            int genuineOffset = mInputEchoFilter.consumeEchoPrefixReturningGenuineOffset(mReceiveBuffer, 0, bytesRead);
            if (genuineOffset > 0) mEmulator.append(mReceiveBuffer, genuineOffset);
            int genuineByteCount = bytesRead - genuineOffset;
            mEmulator.appendGenuineOutput(mReceiveBuffer, genuineOffset, genuineByteCount);
            notifyScreenUpdate();
            if (genuineByteCount > 0) notifyGenuineOutput();
        }

        private void renderShellCompletionNotice(int exitCode) {
            byte[] bytesToWrite = shellCompletionNotice(exitCode).getBytes(StandardCharsets.UTF_8);
            mEmulator.append(bytesToWrite, bytesToWrite.length);
            notifyScreenUpdate();
        }

        private String shellCompletionNotice(int exitCode) {
            if (exitCode > 0) return "\r\n[Process completed (code " + exitCode + ") - press Enter]";
            if (exitCode < 0) return "\r\n[Process completed (signal " + (-exitCode) + ") - press Enter]";
            return "\r\n[Process completed - press Enter]";
        }

    }

}
