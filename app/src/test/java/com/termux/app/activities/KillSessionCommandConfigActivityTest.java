package com.termux.app.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.widget.EditText;

import com.termux.R;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParser;

@RunWith(RobolectricTestRunner.class)
public class KillSessionCommandConfigActivityTest {

    private static boolean rootPreferencesDeclareKey(Context context, String preferenceKey) throws Exception {
        try (XmlResourceParser parser = context.getResources().getXml(R.xml.root_preferences)) {
            for (int event = parser.next(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                if (event != XmlPullParser.START_TAG) continue;
                for (int index = 0; index < parser.getAttributeCount(); index++) {
                    if (preferenceKey.equals(parser.getAttributeValue(index))) return true;
                }
            }
        }
        return false;
    }

    @Test
    public void settingsRootScreenOffersTheKillSessionCommandConfigEntry() throws Exception {
        Assert.assertTrue(rootPreferencesDeclareKey(RuntimeEnvironment.getApplication(),
            "kill_session_command_config"));
    }

    @Test
    public void configActivityIsDeclaredButNotExported() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        ActivityInfo activityInfo = context.getPackageManager().getActivityInfo(
            new ComponentName(context, KillSessionCommandConfigActivity.class), PackageManager.GET_META_DATA);
        Assert.assertFalse("the config screen must not be launchable by other applications",
            activityInfo.exported);
    }

    @Test
    public void killSessionCommandKeyIsStable() {
        Assert.assertEquals("kill_session_command",
            TermuxPreferenceConstants.TERMUX_APP.KEY_KILL_SESSION_COMMAND);
    }

    @Test
    public void killSessionCommandDefaultValueIsEmpty() {
        Assert.assertEquals("", TermuxPreferenceConstants.TERMUX_APP.DEFAULT_VALUE_KEY_KILL_SESSION_COMMAND);
    }

    @Test
    public void saveButtonPersistsCommandToPreferences() {
        KillSessionCommandConfigActivity activity = Robolectric.buildActivity(KillSessionCommandConfigActivity.class)
            .create().start().resume().get();

        EditText input = activity.findViewById(R.id.kill_session_command_input);
        input.setText("ssh host tmux kill-session -t {name}");
        activity.findViewById(R.id.kill_session_command_config_save_button).performClick();

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        Assert.assertEquals("ssh host tmux kill-session -t {name}", prefs.getKillSessionCommand());
    }

    @Test
    public void onCreateLoadsStoredCommandIntoEditText() {
        KillSessionCommandConfigActivity setupActivity =
            Robolectric.buildActivity(KillSessionCommandConfigActivity.class).create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(setupActivity, true);
        Assert.assertNotNull(prefs);
        prefs.setKillSessionCommand("stored-kill-command");

        KillSessionCommandConfigActivity activity =
            Robolectric.buildActivity(KillSessionCommandConfigActivity.class).create().start().resume().get();
        EditText input = activity.findViewById(R.id.kill_session_command_input);
        Assert.assertEquals("stored-kill-command", input.getText().toString());
    }

    @Test
    public void killSessionCommandDefaultsToEmptySoTheActionReportsNotConfiguredUntilItIsSet() {
        KillSessionCommandConfigActivity activity =
            Robolectric.buildActivity(KillSessionCommandConfigActivity.class).create().start().resume().get();
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(activity, true);
        Assert.assertNotNull(prefs);
        prefs.setKillSessionCommand("");

        Assert.assertEquals("", prefs.getKillSessionCommand());
    }
}
