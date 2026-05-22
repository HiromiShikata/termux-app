package com.termux.shared.termux.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.environment.AndroidShellEnvironment;
import com.termux.shared.shell.command.environment.ShellEnvironmentUtils;
import com.termux.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.termux.shared.termux.TermuxBootstrap;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.TermuxShellUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Environment for Termux.
 */
public class TermuxShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "TermuxShellEnvironment";

    /** Environment variable for the termux {@link TermuxConstants#TERMUX_PREFIX_DIR_PATH}. */
    public static final String ENV_PREFIX = "PREFIX";

    public static final String ENV_TERMUX_ROOTFS = "TERMUX__ROOTFS";

    public static final String ENV_TERMUX_PREFIX = "TERMUX__PREFIX";

    public static final String ENV_TERMUX_APP_DATA_DIR = "TERMUX_APP__DATA_DIR";

    public static final String ENV_TERMUX_APP_LEGACY_DATA_DIR = "TERMUX_APP__LEGACY_DATA_DIR";

    public static final String ENV_TERMUX_EXEC_SYSTEM_LINKER_EXEC_MODE = "TERMUX_EXEC__SYSTEM_LINKER_EXEC__MODE";

    public static final String ENV_LD_PRELOAD = "LD_PRELOAD";

    public static final String TERMUX_EXEC_SYSTEM_LINKER_EXEC_MODE_FORCE = "force";

    public static final String LIBTERMUX_EXEC_LD_PRELOAD_FILE_NAME = "libtermux-exec-ld-preload.so";

    public TermuxShellEnvironment() {
        super();
        shellCommandShellEnvironment = new TermuxShellCommandShellEnvironment();
    }


    /** Init {@link TermuxShellEnvironment} constants and caches. */
    public synchronized static void init(@NonNull Context currentPackageContext) {
        TermuxAppShellEnvironment.setTermuxAppEnvironment(currentPackageContext);
    }

    /** Init {@link TermuxShellEnvironment} constants and caches. */
    public synchronized static void writeEnvironmentToFile(@NonNull Context currentPackageContext) {
        HashMap<String, String> environmentMap = new TermuxShellEnvironment().getEnvironment(currentPackageContext, false);
        String environmentString = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(environmentMap);

        // Write environment string to temp file and then move to final location since otherwise
        // writing may happen while file is being sourced/read
        Error error = FileUtils.writeTextToFile("termux.env.tmp", TermuxConstants.TERMUX_ENV_TEMP_FILE_PATH,
            Charset.defaultCharset(), environmentString, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return;
        }

        error = FileUtils.moveRegularFile("termux.env.tmp", TermuxConstants.TERMUX_ENV_TEMP_FILE_PATH, TermuxConstants.TERMUX_ENV_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    /** Get shell environment for Termux. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {

        // Termux environment builds upon the Android environment
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, isFailSafe);

        HashMap<String, String> termuxAppEnvironment = TermuxAppShellEnvironment.getEnvironment(currentPackageContext);
        if (termuxAppEnvironment != null)
            environment.putAll(termuxAppEnvironment);

        HashMap<String, String> termuxApiAppEnvironment = TermuxAPIShellEnvironment.getEnvironment(currentPackageContext);
        if (termuxApiAppEnvironment != null)
            environment.putAll(termuxApiAppEnvironment);

        environment.put(ENV_HOME, TermuxConstants.TERMUX_HOME_DIR_PATH);
        environment.put(ENV_PREFIX, TermuxConstants.TERMUX_PREFIX_DIR_PATH);

        // If failsafe is not enabled, then we keep default PATH and TMPDIR so that system binaries can be used
        if (!isFailSafe) {
            environment.put(ENV_TMPDIR, TermuxConstants.TERMUX_TMP_PREFIX_DIR_PATH);
            if (TermuxBootstrap.isAppPackageVariantAPTAndroid5()) {
                // Termux in android 5/6 era shipped busybox binaries in applets directory
                environment.put(ENV_PATH, TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + ":" + TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/applets");
                environment.put(ENV_LD_LIBRARY_PATH, TermuxConstants.TERMUX_LIB_PREFIX_DIR_PATH);
            } else {
                // Bootstrap ELF binaries carry a DT_RUNPATH baked at upstream build time pointing at
                // the upstream prefix /data/data/com.termux/files/usr/lib, which does not exist for
                // this fork's package name. The bionic linker searches LD_LIBRARY_PATH before
                // DT_RUNPATH, so exporting this fork's real lib directory makes libraries such as
                // libmd.so resolvable.
                environment.put(ENV_PATH, TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH);
                environment.put(ENV_LD_LIBRARY_PATH, TermuxConstants.TERMUX_LIB_PREFIX_DIR_PATH);
            }

            // Tell libtermux-exec-ld-preload.so where this fork's data directory lives. Without
            // these variables the library falls back to the upstream-compiled defaults that point
            // at /data/data/com.termux/... which does not exist for this fork. Both the user-0
            // path and the legacy path are exported because the library matches the executable path
            // against both when deciding whether to apply system linker exec.
            environment.put(ENV_TERMUX_ROOTFS, TermuxConstants.TERMUX_FILES_DIR_PATH);
            environment.put(ENV_TERMUX_PREFIX, TermuxConstants.TERMUX_PREFIX_DIR_PATH);
            environment.put(ENV_TERMUX_APP_DATA_DIR, TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH_USER_0);
            environment.put(ENV_TERMUX_APP_LEGACY_DATA_DIR, TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);
            environment.put(ENV_TERMUX_EXEC_SYSTEM_LINKER_EXEC_MODE, TERMUX_EXEC_SYSTEM_LINKER_EXEC_MODE_FORCE);

            File termuxExecLib = new File(TermuxConstants.TERMUX_LIB_PREFIX_DIR_PATH + "/" + LIBTERMUX_EXEC_LD_PRELOAD_FILE_NAME);
            if (termuxExecLib.isFile()) {
                environment.put(ENV_LD_PRELOAD, termuxExecLib.getAbsolutePath());
            }
        }

        return environment;
    }


    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return TermuxConstants.TERMUX_HOME_DIR_PATH;
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
    }

    @NonNull
    @Override
    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
        return TermuxShellUtils.wrapWithSystemLinkerIfRequired(
            TermuxShellUtils.setupShellCommandArguments(executable, arguments));
    }

}
