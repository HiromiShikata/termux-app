package com.termux.app.diagnostics;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.app.TermuxActivity;

public final class DiagnosticsShareAction {

    @NonNull
    private final DiagnosticsReportCollector mCollector;
    @NonNull
    private final DiagnosticsReportBuilder mReportBuilder;

    public DiagnosticsShareAction() {
        this(new DiagnosticsReportCollector(), new DiagnosticsReportBuilder());
    }

    public DiagnosticsShareAction(@NonNull DiagnosticsReportCollector collector,
                                  @NonNull DiagnosticsReportBuilder reportBuilder) {
        mCollector = collector;
        mReportBuilder = reportBuilder;
    }

    public void shareFrom(@NonNull TermuxActivity activity) {
        DiagnosticsReport report = mCollector.collect(activity, System.currentTimeMillis());
        String reportText = mReportBuilder.build(report);

        copyToClipboard(activity, reportText);
        openShareSheet(activity, reportText);
        Toast.makeText(activity, R.string.msg_diagnostics_copied, Toast.LENGTH_SHORT).show();
    }

    private void copyToClipboard(@NonNull Context context, @NonNull String reportText) {
        ClipboardManager clipboardManager =
            (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) return;
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(context.getString(R.string.share_diagnostics_preference_title), reportText));
    }

    private void openShareSheet(@NonNull Context context, @NonNull String reportText) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, reportText);
        Intent chooserIntent = Intent.createChooser(
            sendIntent, context.getString(R.string.share_diagnostics_preference_title));
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }
}
