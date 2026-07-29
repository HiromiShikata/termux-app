package com.termux.app.activities;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.theme.NightMode;

public class KillSessionCommandConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_kill_session_command_config);
        setTitle(R.string.title_kill_session_command_config);

        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        EditText commandInput = findViewById(R.id.kill_session_command_input);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(this, true);
        if (preferences == null) {
            finish();
            return;
        }

        commandInput.setText(preferences.getKillSessionCommand());

        findViewById(R.id.kill_session_command_config_save_button).setOnClickListener(v -> {
            preferences.setKillSessionCommand(commandInput.getText().toString());
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
