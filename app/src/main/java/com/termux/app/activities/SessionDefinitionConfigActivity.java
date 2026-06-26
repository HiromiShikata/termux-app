package com.termux.app.activities;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;
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
        EditText reloadIntervalInput = findViewById(R.id.session_definition_reload_interval_input);
        EditText maxSessionsInput = findViewById(R.id.session_definition_max_sessions_input);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(this, true);
        if (preferences == null) {
            finish();
            return;
        }

        urlInput.setText(preferences.getSessionDefinitionUrl());
        reloadIntervalInput.setText(String.valueOf(preferences.getSessionDefinitionReloadIntervalMinutes()));
        maxSessionsInput.setText(String.valueOf(preferences.getSessionDefinitionMaxSessions()));

        findViewById(R.id.session_definition_config_save_button).setOnClickListener(v -> {
            preferences.setSessionDefinitionUrl(urlInput.getText().toString().trim());
            preferences.setSessionDefinitionReloadIntervalMinutes(
                parseReloadIntervalMinutes(reloadIntervalInput.getText().toString()));
            preferences.setSessionDefinitionMaxSessions(
                parseMaxSessions(maxSessionsInput.getText().toString()));
            finish();
        });
    }

    static int parseReloadIntervalMinutes(String value) {
        if (value == null) {
            return 0;
        }
        try {
            int minutes = Integer.parseInt(value.trim());
            return minutes < 0 ? 0 : minutes;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static int parseMaxSessions(String value) {
        if (value == null) {
            return TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS;
        }
        try {
            int maxSessions = Integer.parseInt(value.trim());
            return maxSessions < TermuxPreferenceConstants.TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS
                ? TermuxPreferenceConstants.TERMUX_APP.MINIMUM_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS : maxSessions;
        } catch (NumberFormatException e) {
            return TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_MAX_SESSIONS;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
