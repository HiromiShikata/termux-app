package com.termux.shared.termux.crash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.termux.shared.R;
import com.termux.shared.interact.ShareUtils;

public class CrashReportCopyBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_REPORT_TEXT = "com.termux.shared.termux.crash.EXTRA_REPORT_TEXT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String reportText = intent.getStringExtra(EXTRA_REPORT_TEXT);
        if (reportText == null) return;
        ShareUtils.copyFullTextToClipboard(context, null, reportText,
            context.getString(R.string.msg_crash_report_copied));
    }
}
