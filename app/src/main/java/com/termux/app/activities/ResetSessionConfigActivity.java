package com.termux.app.activities;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.theme.NightMode;

public class ResetSessionConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_reset_session_config);
        setTitle(R.string.title_reset_session_config);

        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        EditText commandInput = findViewById(R.id.reset_session_command_input);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(this, true);
        if (preferences == null) {
            finish();
            return;
        }

        commandInput.setText(preferences.getResetSessionCommand());

        findViewById(R.id.reset_session_config_save_button).setOnClickListener(v -> {
            preferences.setResetSessionCommand(commandInput.getText().toString());
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
