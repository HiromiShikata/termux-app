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
public class AutosshConfigActivityTest {

    @Test
    public void autosshCommandKeyIsStable() {
        Assert.assertEquals("autossh_command", TermuxPreferenceConstants.TERMUX_APP.KEY_AUTOSSH_COMMAND);
    }

    @Test
    public void autosshCommandDefaultValueIsEmpty() {
        Assert.assertEquals("", TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_AUTOSSH_COMMAND);
    }

    @Test
    public void saveButtonPersistsCommandToPreferences() {
        AutosshConfigActivity activity = Robolectric.buildActivity(AutosshConfigActivity.class)
            .create().start().resume().get();

        EditText input = activity.findViewById(R.id.autossh_command_input);
        input.setText("autossh -M 0 {url}");
        activity.findViewById(R.id.autossh_config_save_button).performClick();

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals("autossh -M 0 {url}", prefs.getAutosshCommand());
    }

    @Test
    public void onCreateLoadsStoredCommandIntoEditText() {
        AutosshConfigActivity setupActivity = Robolectric.buildActivity(AutosshConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(setupActivity, true);
        Assert.assertNotNull(prefs);
        prefs.setAutosshCommand("stored-command");

        AutosshConfigActivity activity = Robolectric.buildActivity(AutosshConfigActivity.class)
            .create().start().resume().get();
        EditText input = activity.findViewById(R.id.autossh_command_input);
        Assert.assertEquals("stored-command", input.getText().toString());
    }

}
