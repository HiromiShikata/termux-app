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

    @Test
    public void reloadIntervalKeyIsStable() {
        Assert.assertEquals("session_definition_reload_interval_minutes",
            TermuxPreferenceConstants.TERMUX_APP.KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES);
    }

    @Test
    public void reloadIntervalDefaultValueIsZero() {
        Assert.assertEquals(0,
            TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_SESSION_DEFINITION_RELOAD_INTERVAL_MINUTES);
    }

    @Test
    public void reloadIntervalGetReturnsDefaultWhenUnset() {
        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals(0, prefs.getSessionDefinitionReloadIntervalMinutes());
    }

    @Test
    public void reloadIntervalSetThenGetRoundTrips() {
        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        prefs.setSessionDefinitionReloadIntervalMinutes(15);
        Assert.assertEquals(15, prefs.getSessionDefinitionReloadIntervalMinutes());
    }

    @Test
    public void reloadIntervalNegativeIsClampedToZeroOnSet() {
        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        prefs.setSessionDefinitionReloadIntervalMinutes(-5);
        Assert.assertEquals(0, prefs.getSessionDefinitionReloadIntervalMinutes());
    }

    @Test
    public void saveButtonPersistsReloadIntervalToPreferences() {
        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();

        EditText intervalInput = activity.findViewById(R.id.session_definition_reload_interval_input);
        intervalInput.setText("10");
        activity.findViewById(R.id.session_definition_config_save_button).performClick();

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals(10, prefs.getSessionDefinitionReloadIntervalMinutes());
    }

    @Test
    public void onCreateLoadsStoredReloadIntervalIntoEditText() {
        SessionDefinitionConfigActivity setupActivity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(setupActivity, true);
        Assert.assertNotNull(prefs);
        prefs.setSessionDefinitionReloadIntervalMinutes(7);

        SessionDefinitionConfigActivity activity = Robolectric.buildActivity(SessionDefinitionConfigActivity.class)
            .create().start().resume().get();
        EditText intervalInput = activity.findViewById(R.id.session_definition_reload_interval_input);
        Assert.assertEquals("7", intervalInput.getText().toString());
    }

    @Test
    public void parseReloadIntervalMinutesHandlesBlankAndInvalidAsZero() {
        Assert.assertEquals(0, SessionDefinitionConfigActivity.parseReloadIntervalMinutes(""));
        Assert.assertEquals(0, SessionDefinitionConfigActivity.parseReloadIntervalMinutes("   "));
        Assert.assertEquals(0, SessionDefinitionConfigActivity.parseReloadIntervalMinutes("abc"));
        Assert.assertEquals(0, SessionDefinitionConfigActivity.parseReloadIntervalMinutes(null));
        Assert.assertEquals(0, SessionDefinitionConfigActivity.parseReloadIntervalMinutes("-3"));
    }

    @Test
    public void parseReloadIntervalMinutesParsesValidNumber() {
        Assert.assertEquals(12, SessionDefinitionConfigActivity.parseReloadIntervalMinutes("12"));
        Assert.assertEquals(3, SessionDefinitionConfigActivity.parseReloadIntervalMinutes("  3  "));
    }
}
