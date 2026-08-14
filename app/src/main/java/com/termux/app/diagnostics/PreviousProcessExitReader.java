package com.termux.app.diagnostics;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public final class PreviousProcessExitReader {

    private static final int MOST_RECENT_ENDINGS_READ = 5;

    private static final int EVERY_PROCESS_OF_THIS_APP = 0;

    @NonNull
    public DiagnosticsPreviousProcessExits read(@NonNull Context context, int androidVersion) {
        if (!ProcessExitReasonAvailability.isRecordedBy(androidVersion)) {
            return DiagnosticsPreviousProcessExits.notRecordedByThisAndroid();
        }
        return readTheRecordAndroidKeeps(context);
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.R)
    private DiagnosticsPreviousProcessExits readTheRecordAndroidKeeps(@NonNull Context context) {
        ActivityManager activityManager =
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return DiagnosticsPreviousProcessExits.NOT_TAKEN;
        List<ApplicationExitInfo> recordedEndings = activityManager.getHistoricalProcessExitReasons(
            null, EVERY_PROCESS_OF_THIS_APP, MOST_RECENT_ENDINGS_READ);
        List<DiagnosticsPreviousProcessExit> endings = new ArrayList<>();
        for (ApplicationExitInfo recordedEnding : recordedEndings) {
            endings.add(new DiagnosticsPreviousProcessExit(recordedEnding.getTimestamp(),
                recordedEnding.getReason(), recordedEnding.getImportance(),
                recordedEnding.getDescription()));
        }
        return DiagnosticsPreviousProcessExits.recorded(endings);
    }
}
