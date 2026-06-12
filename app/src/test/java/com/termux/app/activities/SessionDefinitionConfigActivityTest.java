package com.termux.app.activities;

import android.widget.EditText;

import com.termux.R;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionConfigActivityTest {

    @Test
    public void sessionDefinitionUrlKeyIsStable() {
        Assert.assertEquals("session_definition_url", TermuxPreferenceConstants.TERMUX_APP.KEY_SESSION_DEFINITION_URL);
    }

    @Test
    public void sessionDefinitionUrlDefaultValueIsEmpty() {
        Assert.assertEquals("", TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_URL);
    }

    @Test
    public void saveButtonPersistsUrlToPreferences() {
        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();

        EditText input = activity.findViewById(R.id.session_definition_url_input);
        input.setText("https://example.test/base/index.json");
        activity.findViewById(R.id.session_definition_config_save_button).performClick();

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals("https://example.test/base/index.json", prefs.getSessionDefinitionUrl());
    }

    @Test
    public void saveButtonTrimsUrlBeforePersisting() {
        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();

        EditText input = activity.findViewById(R.id.session_definition_url_input);
        input.setText("  https://example.test/base/index.json  ");
        activity.findViewById(R.id.session_definition_config_save_button).performClick();

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals("https://example.test/base/index.json", prefs.getSessionDefinitionUrl());
    }

    @Test
    public void onCreateLoadsStoredUrlIntoEditText() {
        SessionDefinitionConfigActivity setupActivity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(setupActivity, true);
        Assert.assertNotNull(prefs);
        prefs.setSessionDefinitionUrl("https://example.test/stored/index.json");

        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        EditText input = activity.findViewById(R.id.session_definition_url_input);
        Assert.assertEquals("https://example.test/stored/index.json", input.getText().toString());
    }
}
