package com.termux.app.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.interact.ShareUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.theme.NightMode;

public class CrashLogViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_crash_log_viewer);
        setTitle(R.string.title_crash_log_viewer);

        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        String crashLog = readCrashLog();

        TextView crashLogText = findViewById(R.id.crash_log_viewer_text);
        MaterialButton copyButton = findViewById(R.id.crash_log_viewer_copy_button);

        if (crashLog.isEmpty()) {
            crashLogText.setText(R.string.msg_crash_log_not_found);
            copyButton.setEnabled(false);
        } else {
            crashLogText.setText(crashLog);
            copyButton.setOnClickListener(v ->
                ShareUtils.copyTextToClipboard(this, crashLog, getString(R.string.msg_crash_log_copied)));
        }
    }

    private String readCrashLog() {
        String log = readTextFromFile(TermuxConstants.TERMUX_CRASH_LOG_FILE_PATH);
        if (log.isEmpty())
            log = readTextFromFile(TermuxConstants.TERMUX_CRASH_LOG_BACKUP_FILE_PATH);
        return log;
    }

    private String readTextFromFile(String filePath) {
        StringBuilder builder = new StringBuilder();
        Error error = FileUtils.readTextFromFile("crash log", filePath, null, builder, true);
        if (error != null)
            return "";
        return builder.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
