package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.system.Os;
import android.util.Pair;
import android.view.WindowManager;

import com.termux.R;
import com.termux.shared.file.FileUtils;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.interact.MessageDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.markdown.MarkdownUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.android.PackageUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_PREFIX_DIR_PATH;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR;
import static com.termux.shared.termux.TermuxConstants.TERMUX_STAGING_PREFIX_DIR_PATH;

/**
 * Install the Termux bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX directory below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging directory, $STAGING_PREFIX, is cleared if left over from broken installation below.
 * <p/>
 * (4) The zip file is loaded from a shared library.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 */
final class TermuxInstaller {

    private static final String LOG_TAG = "TermuxInstaller";

    static final String SECOND_STAGE_SCRIPT_RELATIVE_PATH = "etc/termux/termux-bootstrap/second-stage/termux-bootstrap-second-stage.sh";

    static final String SECOND_STAGE_DPKG_VERSION_INVOCATION = "dpkg_version=$(dpkg --version | head -n 1 | sed -E 's/.*version ([^ ]+) .*/\\1/')";

    static final String SECOND_STAGE_DPKG_VERSION_REPLACEMENT_TOKEN = "termux-app-fork-dpkg-version-from-status";

    static final String UPDATE_ALTERNATIVES_FORK_FLAGS_TOKEN = "termux-app-fork-update-alternatives-altdir";

    static final String MAINTAINER_SCRIPT_INSTALL_INVOCATION = "update-alternatives \\\n      --install";

    static final String MAINTAINER_SCRIPT_REMOVE_INVOCATION = "update-alternatives --remove";

    static final String FORK_EACCES_SHIM_LIBRARY_NAME = "libtermux-fork-eacces-shim.so";

    static final String FORK_EACCES_SHIM_PREFIX_RELATIVE_PATH = "lib/" + FORK_EACCES_SHIM_LIBRARY_NAME;

    /** Performs bootstrap setup if necessary. */
    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        String bootstrapErrorMessage;
        Error filesDirectoryAccessibleError;

        // This will also call Context.getFilesDir(), which should ensure that termux files directory
        // is created if it does not already exist
        filesDirectoryAccessibleError = TermuxFileUtils.isTermuxFilesDirectoryAccessible(activity, true, true);
        boolean isFilesDirectoryAccessible = filesDirectoryAccessibleError == null;

        // Termux can only be run as the primary user (device owner) since only that
        // account has the expected file system paths. Verify that:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !PackageUtils.isCurrentUserThePrimaryUser(activity)) {
            bootstrapErrorMessage = activity.getString(R.string.bootstrap_error_not_primary_user_message,
                MarkdownUtils.getMarkdownCodeForString(TERMUX_PREFIX_DIR_PATH, false));
            Logger.logError(LOG_TAG, "isFilesDirectoryAccessible: " + isFilesDirectoryAccessible);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.exitAppWithErrorMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage);
            return;
        }

        if (!isFilesDirectoryAccessible) {
            bootstrapErrorMessage = Error.getMinimalErrorString(filesDirectoryAccessibleError);
            //noinspection SdCardPath
            if (PackageUtils.isAppInstalledOnExternalStorage(activity) &&
                !TermuxConstants.TERMUX_FILES_DIR_PATH.equals(activity.getFilesDir().getAbsolutePath().replaceAll("^/data/user/0/", "/data/data/"))) {
                bootstrapErrorMessage += "\n\n" + activity.getString(R.string.bootstrap_error_installed_on_portable_sd,
                    MarkdownUtils.getMarkdownCodeForString(TERMUX_PREFIX_DIR_PATH, false));
            }

            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.showMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage, null);
            return;
        }

        // If prefix directory exists, even if its a symlink to a valid directory and symlink is not broken/dangling
        if (FileUtils.directoryFileExists(TERMUX_PREFIX_DIR_PATH, true)) {
            if (TermuxFileUtils.isTermuxPrefixDirectoryEmpty()) {
                Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" exists but is empty or only contains specific unimportant files.");
            } else if (!bootstrapLoginShebangMatchesCurrentPackage()) {
                Logger.logInfo(LOG_TAG, "Reinstalling bootstrap: login script requires update for current package.");
            } else if (!bootstrapSecondStageScriptIsPatchedForFork()) {
                Logger.logInfo(LOG_TAG, "Reinstalling bootstrap: second-stage script requires update to bypass dpkg --version on fork installs.");
            } else if (!bootstrapMaintainerScriptsArePatchedForFork()) {
                Logger.logInfo(LOG_TAG, "Reinstalling bootstrap: maintainer scripts require update-alternatives --altdir / --admindir injection for fork installs.");
            } else {
                ensureHomeDirectoryConfigFiles();
                ensureForkEaccesShim(activity);
                whenDone.run();
                return;
            }
        } else if (FileUtils.fileExists(TERMUX_PREFIX_DIR_PATH, false)) {
            Logger.logInfo(LOG_TAG, "The termux prefix directory \"" + TERMUX_PREFIX_DIR_PATH + "\" does not exist but another file exists at its destination.");
        }

        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    Logger.logInfo(LOG_TAG, "Installing " + TermuxConstants.TERMUX_APP_NAME + " bootstrap packages.");

                    Error error;

                    // Delete prefix staging directory or any file at its destination
                    error = FileUtils.deleteFile("termux prefix staging directory", TERMUX_STAGING_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Delete prefix directory or any file at its destination
                    error = FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix staging directory if it does not already exist and set required permissions
                    error = TermuxFileUtils.isTermuxPrefixStagingDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix directory if it does not already exist and set required permissions
                    error = TermuxFileUtils.isTermuxPrefixDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Extracting bootstrap zip to prefix staging directory \"" + TERMUX_STAGING_PREFIX_DIR_PATH + "\".");

                    final byte[] buffer = new byte[8096];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(50);

                    final byte[] zipBytes = loadZipBytes();
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = symlinksReader.readLine()) != null) {
                                    String[] parts = line.split("←");
                                    if (parts.length != 2)
                                        throw new RuntimeException("Malformed symlink line: " + line);
                                    String oldPath = patchPackagePathString(parts[0]);
                                    String newPath = TERMUX_STAGING_PREFIX_DIR_PATH + "/" + parts[1];
                                    symlinks.add(Pair.create(oldPath, newPath));

                                    error = ensureDirectoryExists(new File(newPath).getParentFile());
                                    if (error != null) {
                                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                        return;
                                    }
                                }
                            } else {
                                String zipEntryName = zipEntry.getName();
                                File targetFile = new File(TERMUX_STAGING_PREFIX_DIR_PATH, zipEntryName);
                                boolean isDirectory = zipEntry.isDirectory();

                                error = ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());
                                if (error != null) {
                                    showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                    return;
                                }

                                if (!isDirectory) {
                                    int firstRead = zipInput.read(buffer);
                                    if (isElfHeader(buffer, firstRead)) {
                                        try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                            if (firstRead > 0) outStream.write(buffer, 0, firstRead);
                                            int readBytes;
                                            while ((readBytes = zipInput.read(buffer)) != -1)
                                                outStream.write(buffer, 0, readBytes);
                                        }
                                    } else {
                                        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
                                        if (firstRead > 0) contentBuffer.write(buffer, 0, firstRead);
                                        int readBytes;
                                        while ((readBytes = zipInput.read(buffer)) != -1)
                                            contentBuffer.write(buffer, 0, readBytes);
                                        byte[] fileBytes = patchPackagePathsIfNeeded(contentBuffer.toByteArray());
                                        if (zipEntryName.equals("bin/login") && !TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux")) {
                                            fileBytes = patchLoginScriptForFork(fileBytes);
                                        }
                                        if (zipEntryName.equals(SECOND_STAGE_SCRIPT_RELATIVE_PATH) && !TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux")) {
                                            fileBytes = patchSecondStageScriptForFork(fileBytes);
                                        }
                                        if (isDpkgMaintainerScriptEntry(zipEntryName) && !TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux")) {
                                            fileBytes = patchUpdateAlternativesInvocations(fileBytes);
                                        }
                                        try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                            outStream.write(fileBytes);
                                        }
                                    }
                                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") ||
                                        zipEntryName.startsWith("lib/apt/apt-helper") || zipEntryName.startsWith("lib/apt/methods")) {
                                        //noinspection OctalInteger
                                        Os.chmod(targetFile.getAbsolutePath(), 0700);
                                    }
                                }
                            }
                        }
                    }

                    if (symlinks.isEmpty())
                        throw new RuntimeException("No SYMLINKS.txt encountered");
                    for (Pair<String, String> symlink : symlinks) {
                        Os.symlink(symlink.first, symlink.second);
                    }

                    Logger.logInfo(LOG_TAG, "Moving termux prefix staging to prefix directory.");

                    if (!TERMUX_STAGING_PREFIX_DIR.renameTo(TERMUX_PREFIX_DIR)) {
                        throw new RuntimeException("Moving termux prefix staging to prefix directory failed");
                    }

                    Logger.logInfo(LOG_TAG, "Bootstrap packages installed successfully.");

                    ensureHomeDirectoryConfigFiles();
                    ensureForkEaccesShim(activity);

                    // Recreate env file since termux prefix was wiped earlier
                    TermuxShellEnvironment.writeEnvironmentToFile(activity);

                    activity.runOnUiThread(whenDone);

                } catch (final Exception e) {
                    showBootstrapErrorDialog(activity, whenDone, Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)));

                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    public static void showBootstrapErrorDialog(Activity activity, Runnable whenDone, String message) {
        Logger.logErrorExtended(LOG_TAG, "Bootstrap Error:\n" + message);

        // Send a notification with the exception so that the user knows why bootstrap setup failed
        sendBootstrapCrashReportNotification(activity, message);

        activity.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_body)
                    .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                        dialog.dismiss();
                        activity.finish();
                    })
                    .setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                        dialog.dismiss();
                        FileUtils.deleteFile("termux prefix directory", TERMUX_PREFIX_DIR_PATH, true);
                        TermuxInstaller.setupBootstrapIfNeeded(activity, whenDone);
                    }).show();
            } catch (WindowManager.BadTokenException e1) {
                // Activity already dismissed - ignore.
            }
        });
    }

    private static void sendBootstrapCrashReportNotification(Activity activity, String message) {
        final String title = TermuxConstants.TERMUX_APP_NAME + " Bootstrap Error";

        // Add info of all install Termux plugin apps as well since their target sdk or installation
        // on external/portable sd card can affect Termux app files directory access or exec.
        TermuxCrashUtils.sendCrashReportNotification(activity, LOG_TAG,
            title, null, "## " + title + "\n\n" + message + "\n\n" +
                TermuxUtils.getTermuxDebugMarkdownString(activity),
            true, false, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES, true);
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "termux-storage";
        final String title = TermuxConstants.TERMUX_APP_NAME + " Setup Storage Error";

        Logger.logInfo(LOG_TAG, "Setting up storage symlinks.");

        new Thread() {
            public void run() {
                try {
                    Error error;
                    File storageDir = TermuxConstants.TERMUX_STORAGE_HOME_DIR;

                    error = FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    if (error != null) {
                        Logger.logErrorAndShowToast(context, LOG_TAG, error.getMessage());
                        Logger.logErrorExtended(LOG_TAG, "Setup Storage Error\n" + error.toString());
                        TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                            "## " + title + "\n\n" + Error.getErrorMarkdownString(error),
                            true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/shared, ~/storage/downloads, ~/storage/dcim, ~/storage/pictures, ~/storage/music and ~/storage/movies for directories in \"" + Environment.getExternalStorageDirectory().getAbsolutePath() + "\".");

                    // Get primary storage root "/storage/emulated/0" symlink
                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    Os.symlink(documentsDir.getAbsolutePath(), new File(storageDir, "documents").getAbsolutePath());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    Os.symlink(downloadsDir.getAbsolutePath(), new File(storageDir, "downloads").getAbsolutePath());

                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    Os.symlink(dcimDir.getAbsolutePath(), new File(storageDir, "dcim").getAbsolutePath());

                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Os.symlink(picturesDir.getAbsolutePath(), new File(storageDir, "pictures").getAbsolutePath());

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    Os.symlink(musicDir.getAbsolutePath(), new File(storageDir, "music").getAbsolutePath());

                    File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    Os.symlink(moviesDir.getAbsolutePath(), new File(storageDir, "movies").getAbsolutePath());

                    File podcastsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
                    Os.symlink(podcastsDir.getAbsolutePath(), new File(storageDir, "podcasts").getAbsolutePath());

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        File audiobooksDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS);
                        Os.symlink(audiobooksDir.getAbsolutePath(), new File(storageDir, "audiobooks").getAbsolutePath());
                    }

                    // Dir 0 should ideally be for primary storage
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ContextImpl.java;l=818
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=219
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=181
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/StorageManagerService.java;l=3796
                    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/services/core/java/com/android/server/MountService.java;l=3053

                    // Create "Android/data/com.termux" symlinks
                    File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    // Create "Android/media/com.termux" symlinks
                    dirs = context.getExternalMediaDirs();
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "media-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Storage symlinks created successfully.");
                } catch (Exception e) {
                    Logger.logErrorAndShowToast(context, LOG_TAG, e.getMessage());
                    Logger.logStackTraceWithMessage(LOG_TAG, "Setup Storage Error: Error setting up link", e);
                    TermuxCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                        "## " + title + "\n\n" + Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)),
                        true, false, TermuxUtils.AppInfoMode.TERMUX_PACKAGE, true);
                }
            }
        }.start();
    }

    static boolean isElfHeader(byte[] buffer, int length) {
        return length >= 4 && buffer[0] == 0x7F && buffer[1] == 'E' && buffer[2] == 'L' && buffer[3] == 'F';
    }

    static byte[] patchPackagePathsIfNeeded(byte[] fileBytes) {
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        String patched = patchPackagePathString(content);
        if (patched.equals(content)) return fileBytes;
        return patched.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    static String patchPackagePathString(String content) {
        String dataDataPrefix = "/data/data/";
        String newPkgDataPath = TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/";
        String patched = content;
        int searchFrom = 0;
        while (true) {
            int dataDataIdx = patched.indexOf(dataDataPrefix, searchFrom);
            if (dataDataIdx < 0) break;
            int pkgEnd = patched.indexOf('/', dataDataIdx + dataDataPrefix.length());
            if (pkgEnd < 0) break;
            String oldPkgDataPath = patched.substring(dataDataIdx, pkgEnd + 1);
            if (oldPkgDataPath.equals(newPkgDataPath)) {
                searchFrom = pkgEnd + 1;
                continue;
            }
            patched = patched.replace(oldPkgDataPath, newPkgDataPath);
            searchFrom = dataDataIdx + newPkgDataPath.length();
        }
        return patched;
    }

    static byte[] patchLoginScriptForFork(byte[] fileBytes) {
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        String loginShellExecLine = "\texec \"$SHELL\" -l \"$@\"";
        if (!content.contains(loginShellExecLine)) return fileBytes;
        String replacement = "\tif [ \"${SHELL##*/}\" = bash ]; then\n"
            + "\t\texec \"$SHELL\" --init-file \"" + TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/etc/profile\" \"$@\"\n"
            + "\telse\n"
            + "\t\texec \"$SHELL\" -l \"$@\"\n"
            + "\tfi";
        String patched = content.replace(loginShellExecLine, replacement);
        return patched.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    static byte[] patchSecondStageScriptForFork(byte[] fileBytes) {
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        if (!content.contains(SECOND_STAGE_DPKG_VERSION_INVOCATION)) return fileBytes;
        String replacement = "dpkg_version=$(awk '/^Package: dpkg$/{f=1;next} f&&/^Version:/{sub(/^Version: */,\"\");print;exit}' \"${TERMUX_PREFIX}/var/lib/dpkg/status\" 2>/dev/null) # " + SECOND_STAGE_DPKG_VERSION_REPLACEMENT_TOKEN;
        String patched = content.replace(SECOND_STAGE_DPKG_VERSION_INVOCATION, replacement);
        return patched.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    static boolean isDpkgMaintainerScriptEntry(String zipEntryName) {
        if (!zipEntryName.startsWith("var/lib/dpkg/info/")) return false;
        return zipEntryName.endsWith(".postinst") || zipEntryName.endsWith(".prerm")
            || zipEntryName.endsWith(".preinst") || zipEntryName.endsWith(".postrm");
    }

    static byte[] patchUpdateAlternativesInvocations(byte[] fileBytes) {
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        boolean hasInstall = content.contains(MAINTAINER_SCRIPT_INSTALL_INVOCATION);
        boolean hasRemove = content.contains(MAINTAINER_SCRIPT_REMOVE_INVOCATION);
        if (!hasInstall && !hasRemove) return fileBytes;
        String prefix = TermuxConstants.TERMUX_PREFIX_DIR_PATH;
        String flags = "--altdir \"" + prefix + "/etc/alternatives\""
            + " --admindir \"" + prefix + "/var/lib/dpkg/alternatives\"";
        String tokenComment = "# " + UPDATE_ALTERNATIVES_FORK_FLAGS_TOKEN;
        String patched = content;
        if (hasInstall) {
            String installReplacement = tokenComment + "\n    update-alternatives " + flags + " \\\n      --install";
            patched = patched.replace(MAINTAINER_SCRIPT_INSTALL_INVOCATION, installReplacement);
        }
        if (hasRemove) {
            String removeReplacement = tokenComment + "\n    update-alternatives " + flags + " --remove";
            patched = patched.replace(MAINTAINER_SCRIPT_REMOVE_INVOCATION, removeReplacement);
        }
        return patched.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static boolean bootstrapMaintainerScriptsArePatchedForFork() {
        if (TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux")) return true;
        File dpkgInfoDir = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/var/lib/dpkg/info");
        if (!dpkgInfoDir.isDirectory()) return true;
        File[] entries = dpkgInfoDir.listFiles();
        if (entries == null) return true;
        for (File entry : entries) {
            String name = entry.getName();
            if (!entry.isFile() || !entry.canRead()) continue;
            if (!name.endsWith(".postinst") && !name.endsWith(".prerm")
                && !name.endsWith(".preinst") && !name.endsWith(".postrm")) continue;
            try (FileInputStream fis = new FileInputStream(entry)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int readBytes;
                while ((readBytes = fis.read(buf)) != -1) {
                    out.write(buf, 0, readBytes);
                }
                String content = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                boolean callsUpdateAlternatives = content.contains(MAINTAINER_SCRIPT_INSTALL_INVOCATION)
                    || content.contains(MAINTAINER_SCRIPT_REMOVE_INVOCATION);
                if (callsUpdateAlternatives && !content.contains(UPDATE_ALTERNATIVES_FORK_FLAGS_TOKEN)) {
                    return false;
                }
            } catch (IOException e) {
                return true;
            }
        }
        return true;
    }

    private static boolean bootstrapSecondStageScriptIsPatchedForFork() {
        if (TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux")) return true;
        File secondStageScript = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/" + SECOND_STAGE_SCRIPT_RELATIVE_PATH);
        if (!secondStageScript.isFile() || !secondStageScript.canRead()) return true;
        try (FileInputStream fis = new FileInputStream(secondStageScript)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int readBytes;
            while ((readBytes = fis.read(buf)) != -1) {
                out.write(buf, 0, readBytes);
            }
            String content = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            return content.contains(SECOND_STAGE_DPKG_VERSION_REPLACEMENT_TOKEN);
        } catch (IOException e) {
            return true;
        }
    }

    private static boolean bootstrapLoginShebangMatchesCurrentPackage() {
        File loginFile = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/login");
        if (!loginFile.isFile() || !loginFile.canRead()) return true;
        try (FileInputStream fis = new FileInputStream(loginFile)) {
            byte[] buf = new byte[65536];
            int bytesRead = fis.read(buf);
            if (bytesRead < 2 || buf[0] != '#' || buf[1] != '!') return true;
            String content = new String(buf, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
            if (!content.contains("/data/data/")) return true;
            int dataDataIdx = content.indexOf("/data/data/");
            int pkgEnd = content.indexOf('/', dataDataIdx + "/data/data/".length());
            if (pkgEnd < 0) return true;
            String oldPkgDataPath = content.substring(dataDataIdx, pkgEnd + 1);
            if (!oldPkgDataPath.startsWith("/data/data/")) return true;
            if (!oldPkgDataPath.equals(TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/")) return false;
            if (!TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux") && !content.contains("--init-file")) return false;
            return true;
        } catch (IOException e) {
            return true;
        }
    }

    private static void ensureHomeDirectoryConfigFiles() {
        String homeDirPath = TermuxConstants.TERMUX_HOME_DIR_PATH;
        String prefixDirPath = TermuxConstants.TERMUX_PREFIX_DIR_PATH;

        new File(homeDirPath).mkdirs();

        String bashProfileContent = "[ -f \"" + prefixDirPath + "/etc/profile\" ] && . \"" + prefixDirPath + "/etc/profile\"\n";
        File bashProfile = new File(homeDirPath, ".bash_profile");
        if (!bashProfile.exists() || !fileContentMatches(bashProfile, bashProfileContent)) {
            try (FileOutputStream fos = new FileOutputStream(bashProfile)) {
                fos.write(bashProfileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to create ~/.bash_profile", e);
            }
        }

        String dpkgCfgContent = "admindir " + prefixDirPath + "/var/lib/dpkg\n";
        File dpkgCfg = new File(homeDirPath, ".dpkg.cfg");
        if (!dpkgCfg.exists() || !fileContentMatches(dpkgCfg, dpkgCfgContent)) {
            try (FileOutputStream fos = new FileOutputStream(dpkgCfg)) {
                fos.write(dpkgCfgContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to create ~/.dpkg.cfg", e);
            }
        }
    }

    private static void ensureForkEaccesShim(Activity activity) {
        if (TermuxConstants.TERMUX_PACKAGE_NAME.equals("com.termux")) return;
        String nativeLibraryDir = activity.getApplicationInfo().nativeLibraryDir;
        if (nativeLibraryDir == null) {
            Logger.logError(LOG_TAG, "nativeLibraryDir is null; cannot stage fork EACCES shim.");
            return;
        }
        File sourceLib = new File(nativeLibraryDir, FORK_EACCES_SHIM_LIBRARY_NAME);
        File targetLibDir = new File(TermuxConstants.TERMUX_LIB_PREFIX_DIR_PATH);
        stageForkEaccesShim(sourceLib, targetLibDir);
    }

    static void stageForkEaccesShim(File sourceLib, File targetLibDir) {
        if (!sourceLib.isFile()) {
            Logger.logError(LOG_TAG, "Fork EACCES shim not found at " + sourceLib.getAbsolutePath());
            return;
        }
        if (!targetLibDir.isDirectory() && !targetLibDir.mkdirs()) {
            Logger.logError(LOG_TAG, "Failed to create fork prefix lib directory " + targetLibDir.getAbsolutePath());
            return;
        }
        File targetLib = new File(targetLibDir, FORK_EACCES_SHIM_LIBRARY_NAME);
        if (targetLib.exists() && targetLib.length() == sourceLib.length() && targetLib.lastModified() >= sourceLib.lastModified()) {
            return;
        }
        try (FileInputStream in = new FileInputStream(sourceLib);
             FileOutputStream out = new FileOutputStream(targetLib)) {
            byte[] buf = new byte[8192];
            int readBytes;
            while ((readBytes = in.read(buf)) != -1) {
                out.write(buf, 0, readBytes);
            }
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to stage fork EACCES shim to " + targetLib.getAbsolutePath(), e);
        }
    }

    private static boolean fileContentMatches(File file, String expectedContent) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int bytesRead = fis.read(buf);
            if (bytesRead < 0) return false;
            String content = new String(buf, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
            return content.equals(expectedContent);
        } catch (IOException e) {
            return false;
        }
    }

    private static Error ensureDirectoryExists(File directory) {
        return FileUtils.createDirectoryFile(directory.getAbsolutePath());
    }

    public static byte[] loadZipBytes() {
        // Only load the shared library when necessary to save memory usage.
        System.loadLibrary("termux-bootstrap");
        return getZip();
    }

    public static native byte[] getZip();

}
