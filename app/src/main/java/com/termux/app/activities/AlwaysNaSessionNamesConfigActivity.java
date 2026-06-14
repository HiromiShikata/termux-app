package com.termux.app.activities;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;
import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.theme.NightMode;

public class AlwaysNaSessionNamesConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_always_na_session_names_config);
        setTitle(R.string.title_always_na_session_names_config);

        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);

        EditText namesInput = findViewById(R.id.always_na_session_names_input);

        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(this, true);
        if (preferences == null) {
            finish();
            return;
        }

        namesInput.setText(preferences.getAlwaysNaSessionNamesText());

        findViewById(R.id.always_na_session_names_config_save_button).setOnClickListener(v -> {
            preferences.setAlwaysNaSessionNames(namesInput.getText().toString());
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
