package com.termux.app.apkupdate;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Installs an APK through a {@link PackageInstaller} session. Unlike the {@code ACTION_VIEW}
 * installer intent, a session lets the caller request a version downgrade (see
 * {@link DowngradeRequestApplier}), which is what makes reverting to an older build possible on this
 * debuggable app. The install outcome is delivered asynchronously to {@link ApkInstallStatusReceiver}
 * through the commit status callback; this class only reports failures that happen before commit.
 */
public class PackageInstallerSessionInstaller {

    private static final String LOG_TAG = "PackageInstallerSessionInstaller";

    public static final String INSTALL_STATUS_ACTION =
        "com.termux.app.apkupdate.APK_INSTALL_STATUS";

    private static final int STREAM_BUFFER_SIZE = 65536;

    public interface InstallListener {
        void onInstallSessionFailed(String message);
    }

    private final Context context;
    private final DowngradeRequestApplier downgradeRequestApplier;
    private final Handler mainHandler;

    public PackageInstallerSessionInstaller(Context context) {
        this(context, new DowngradeRequestApplier());
    }

    public PackageInstallerSessionInstaller(Context context, DowngradeRequestApplier downgradeRequestApplier) {
        this.context = context.getApplicationContext();
        this.downgradeRequestApplier = downgradeRequestApplier;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void install(File apkFile, boolean requestDowngrade, InstallListener listener) {
        new Thread(() -> {
            try {
                commitSession(apkFile, requestDowngrade);
            } catch (IOException | RuntimeException exception) {
                String message = messageOf(exception);
                Logger.logError(LOG_TAG, "Install session failed: " + message);
                mainHandler.post(() -> listener.onInstallSessionFailed(message));
            }
        }).start();
    }

    private void commitSession(File apkFile, boolean requestDowngrade) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sessionParams =
            new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        if (requestDowngrade && !downgradeRequestApplier.applyTo(sessionParams)) {
            Logger.logWarn(LOG_TAG, "Could not request downgrade; install may be refused as a downgrade");
        }

        int sessionId = packageInstaller.createSession(sessionParams);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        try {
            writeApkToSession(session, apkFile);
            session.commit(buildStatusIntentSender(sessionId));
        } finally {
            session.close();
        }
    }

    private void writeApkToSession(PackageInstaller.Session session, File apkFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(apkFile);
             OutputStream outputStream = session.openWrite(apkFile.getName(), 0, apkFile.length())) {
            byte[] chunk = new byte[STREAM_BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                outputStream.write(chunk, 0, read);
            }
            session.fsync(outputStream);
        }
    }

    private android.content.IntentSender buildStatusIntentSender(int sessionId) {
        Intent statusIntent = new Intent(INSTALL_STATUS_ACTION)
            .setPackage(context.getPackageName())
            .setClass(context, ApkInstallStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, statusIntent,
            pendingIntentFlags());
        return pendingIntent.getIntentSender();
    }

    private int pendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage() : exception.toString();
    }
}
