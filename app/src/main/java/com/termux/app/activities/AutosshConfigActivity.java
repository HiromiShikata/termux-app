package com.termux.app.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.settings.preferences.SharedPreferenceUtils;
import com.termux.shared.theme.NightMode;

public class AutosshConfigActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "autossh_config";
    public static final String KEY_COMMAND = "command";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_autossh_config);
        setTitle(R.string.title_autossh_config);

        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        EditText commandInput = findViewById(R.id.autossh_command_input);

        SharedPreferences prefs = SharedPreferenceUtils.getPrivateSharedPreferences(this, PREFS_NAME);
        commandInput.setText(SharedPreferenceUtils.getString(prefs, KEY_COMMAND, "", false));

        findViewById(R.id.autossh_config_save_button).setOnClickListener(v -> {
            SharedPreferenceUtils.setString(prefs, KEY_COMMAND, commandInput.getText().toString(), false);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
