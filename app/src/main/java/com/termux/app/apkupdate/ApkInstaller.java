package com.termux.app.apkupdate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;

public class ApkInstaller {

    private static final String FILE_PROVIDER_AUTHORITY_SUFFIX = ".apkupdate.fileprovider";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private final Context context;

    public ApkInstaller(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean canRequestPackageInstalls() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true;
        }
        return context.getPackageManager().canRequestPackageInstalls();
    }

    public Intent buildInstallUnknownAppsSettingsIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public Intent buildInstallIntent(File apkFile) {
        Uri contentUri = FileProvider.getUriForFile(context,
            context.getPackageName() + FILE_PROVIDER_AUTHORITY_SUFFIX, apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, APK_MIME_TYPE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
