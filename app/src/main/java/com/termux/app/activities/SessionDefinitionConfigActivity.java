package com.termux.app.activities;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.theme.NightMode;

public class SessionDefinitionConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_session_definition_config);
        setTitle(R.string.title_session_definition_config);

        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        EditText urlInput = findViewById(R.id.session_definition_url_input);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(this, true);
        if (preferences == null) {
            finish();
            return;
        }

        urlInput.setText(preferences.getSessionDefinitionUrl());

        findViewById(R.id.session_definition_config_save_button).setOnClickListener(v -> {
            preferences.setSessionDefinitionUrl(urlInput.getText().toString().trim());
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
