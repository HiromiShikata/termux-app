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
public class ResetSessionConfigActivityTest {

    @Test
    public void resetSessionCommandKeyIsStable() {
        Assert.assertEquals("reset_session_command", TermuxPreferenceConstants.TERMUX_APP.KEY_RESET_SESSION_COMMAND);
    }

    @Test
    public void resetSessionCommandDefaultValueIsEmpty() {
        Assert.assertEquals("", TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_RESET_SESSION_COMMAND);
    }

    @Test
    public void saveButtonPersistsCommandToPreferences() {
        ResetSessionConfigActivity activity = Robolectric.buildActivity(ResetSessionConfigActivity.class)
            .create().start().resume().get();

        EditText input = activity.findViewById(R.id.reset_session_command_input);
        input.setText("ssh host reset.sh {name}");
        activity.findViewById(R.id.reset_session_config_save_button).performClick();

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals("ssh host reset.sh {name}", prefs.getResetSessionCommand());
    }

    @Test
    public void onCreateLoadsStoredCommandIntoEditText() {
        ResetSessionConfigActivity setupActivity = Robolectric.buildActivity(ResetSessionConfigActivity.class)
            .create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(setupActivity, true);
        Assert.assertNotNull(prefs);
        prefs.setResetSessionCommand("stored-reset-command");

        ResetSessionConfigActivity activity = Robolectric.buildActivity(ResetSessionConfigActivity.class)
            .create().start().resume().get();
        EditText input = activity.findViewById(R.id.reset_session_command_input);
        Assert.assertEquals("stored-reset-command", input.getText().toString());
    }

}
