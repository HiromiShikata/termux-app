package com.termux.shared.termux.shell;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.errors.Error;
import com.termux.shared.file.filesystem.FileTypes;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;

import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TermuxShellUtils {

    private static final String LOG_TAG = "TermuxShellUtils";

    public static final String SYSTEM_LINKER_64_PATH = "/system/bin/linker64";

    public static final String SYSTEM_LINKER_PATH = "/system/bin/linker";

    /**
     * Setup shell command arguments for the execute. The file interpreter may be prefixed to
     * command arguments if needed.
     *
     * On Android 10 (API 29) and above, the platform blocks execve() on files inside the app's
     * private data directory when targetSdkVersion is also 29 or above. The Termux bootstrap
     * binaries are installed under the app data directory, so a direct execve() of any such file
     * is rejected by the kernel with EACCES ("Permission denied"). The fork raises
     * targetSdkVersion to 35 for Android 15 compatibility, which triggers this restriction.
     *
     * To work around this, any executable or shebang interpreter that resolves to a file inside
     * the Termux private data directory is wrapped by the system dynamic linker
     * ({@link #SYSTEM_LINKER_64_PATH} on 64-bit Android, {@link #SYSTEM_LINKER_PATH} on 32-bit).
     * The kernel allows the exec because the executed file is the system linker, and the linker
     * then loads the original Termux binary in user space, which is permitted.
     */
    @NonNull
    public static String[] setupShellCommandArguments(@NonNull String executable, @Nullable String[] arguments) {
        // The file to execute may either be:
        // - An elf file, in which we execute it directly.
        // - A script file without shebang, which we execute with our standard shell $PREFIX/bin/sh instead of the
        //   system /system/bin/sh. The system shell may vary and may not work at all due to LD_LIBRARY_PATH.
        // - A file with shebang, which we try to handle with e.g. /bin/foo -> $PREFIX/bin/foo.
        String interpreter = null;
        try {
            File file = new File(executable);
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[256];
                int bytesRead = in.read(buffer);
                if (bytesRead > 4) {
                    if (buffer[0] == 0x7F && buffer[1] == 'E' && buffer[2] == 'L' && buffer[3] == 'F') {
                        // Elf file, do nothing.
                    } else if (buffer[0] == '#' && buffer[1] == '!') {
                        // Try to parse shebang.
                        StringBuilder builder = new StringBuilder();
                        for (int i = 2; i < bytesRead; i++) {
                            char c = (char) buffer[i];
                            if (c == ' ' || c == '\n') {
                                if (builder.length() == 0) {
                                    // Skip whitespace after shebang.
                                } else {
                                    // End of shebang.
                                    String shebangExecutable = builder.toString();
                                    if (shebangExecutable.startsWith("/usr") || shebangExecutable.startsWith("/bin")) {
                                        String[] parts = shebangExecutable.split("/");
                                        String binary = parts[parts.length - 1];
                                        interpreter = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/" + binary;
                                    } else if (shebangExecutable.startsWith("/data/data/")) {
                                        interpreter = "/system/bin/sh";
                                    }
                                    break;
                                }
                            } else {
                                builder.append(c);
                            }
                        }
                    } else {
                        // No shebang and no ELF, use standard shell.
                        interpreter = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/sh";
                    }
                }
            }
        } catch (IOException e) {
            // Ignore.
        }

        List<String> result = new ArrayList<>();
        if (interpreter != null) result.add(interpreter);
        result.add(executable);
        if (arguments != null) Collections.addAll(result, arguments);
        return wrapWithSystemLinkerIfRequired(result.toArray(new String[0]));
    }

    /**
     * Wrap the first element of {@code commandArguments} with the system dynamic linker when it
     * resolves to a file inside the Termux private app data directory and the running platform
     * applies the Android 10+ app-data exec restriction.
     *
     * @param commandArguments The command arguments. The first element is the executable.
     * @return The command arguments with the system linker prepended if required, otherwise the
     *         original array.
     */
    @NonNull
    public static String[] wrapWithSystemLinkerIfRequired(@NonNull String[] commandArguments) {
        if (commandArguments.length == 0) return commandArguments;
        if (!isAppDataFileExecRestricted()) return commandArguments;

        String executable = commandArguments[0];
        if (!isPathInsideTermuxAppDataDir(executable)) return commandArguments;

        String systemLinker = resolveSystemLinkerPath();
        if (systemLinker == null) return commandArguments;

        String[] wrapped = new String[commandArguments.length + 1];
        wrapped[0] = systemLinker;
        System.arraycopy(commandArguments, 0, wrapped, 1, commandArguments.length);
        return wrapped;
    }

    public static boolean isAppDataFileExecRestricted() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public static boolean isPathInsideTermuxAppDataDir(@NonNull String path) {
        return path.startsWith(TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/");
    }

    @Nullable
    public static String resolveSystemLinkerPath() {
        File linker64 = new File(SYSTEM_LINKER_64_PATH);
        if (linker64.exists()) return SYSTEM_LINKER_64_PATH;
        File linker32 = new File(SYSTEM_LINKER_PATH);
        if (linker32.exists()) return SYSTEM_LINKER_PATH;
        return null;
    }

    /** Clear files under {@link TermuxConstants#TERMUX_TMP_PREFIX_DIR_PATH}. */
    public static void clearTermuxTMPDIR(boolean onlyIfExists) {
        // Existence check before clearing may be required since clearDirectory() will automatically
        // re-create empty directory if doesn't exist, which should not be done for things like
        // termux-reset (d6eb5e35). Moreover, TMPDIR must be a directory and not a symlink, this can
        // also allow users who don't want TMPDIR to be cleared automatically on termux exit, since
        // it may remove files still being used by background processes (#1159).
        if(onlyIfExists && !FileUtils.directoryFileExists(TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH, false))
            return;

        Error error;

        TermuxAppSharedProperties properties = TermuxAppSharedProperties.getProperties();
        int days = properties.getDeleteTMPDIRFilesOlderThanXDaysOnExit();

        // Disable currently until FileUtils.deleteFilesOlderThanXDays() is fixed.
        if (days > 0)
            days = 0;

        if (days < 0) {
            Logger.logInfo(LOG_TAG, "Not clearing termux $TMPDIR");
        } else if (days == 0) {
            error = FileUtils.clearDirectory("$TMPDIR",
                FileUtils.getCanonicalPath(TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH, null));
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Failed to clear termux $TMPDIR\n" + error);
            }
        } else {
            error = FileUtils.deleteFilesOlderThanXDays("$TMPDIR",
                FileUtils.getCanonicalPath(TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH, null),
                TrueFileFilter.INSTANCE, days, true, FileTypes.FILE_TYPE_ANY_FLAGS);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Failed to delete files from termux $TMPDIR older than " + days + " days\n" + error);
            }
        }
    }

}
