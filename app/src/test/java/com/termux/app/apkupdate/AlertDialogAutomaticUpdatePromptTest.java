package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AlertDialogAutomaticUpdatePromptTest {

    private final List<String> choices = new ArrayList<>();

    private Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).create().start().resume().get();
    }

    private void openPromptOn(Activity activity) {
        new AlertDialogAutomaticUpdatePrompt(activity).openUpdateDialog("1.2.3",
            () -> choices.add("install"), () -> choices.add("cancel"));
    }

    @Test
    public void theDialogOpensCarryingTheVersionThatIsReadyToInstall() {
        openPromptOn(newActivity());

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull("the owner has to see the dialog without pressing anything", dialog);
        Assert.assertTrue("the dialog has to be on screen", dialog.isShowing());
    }

    @Test
    public void pressingInstallRunsTheInstallChoice() {
        openPromptOn(newActivity());

        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        Assert.assertEquals(1, choices.size());
        Assert.assertEquals("install", choices.get(0));
    }

    @Test
    public void pressingLaterRunsTheCancelChoice() {
        openPromptOn(newActivity());

        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

        Assert.assertEquals(1, choices.size());
        Assert.assertEquals("cancel", choices.get(0));
    }

    @Test
    public void dismissingTheDialogRunsTheCancelChoiceToo() {
        openPromptOn(newActivity());

        ShadowAlertDialog.getLatestAlertDialog().cancel();

        Assert.assertEquals(1, choices.size());
        Assert.assertEquals("cancel", choices.get(0));
    }

    @Test
    public void aFinishingActivityOpensNoDialog() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        activity.finish();

        openPromptOn(activity);

        Assert.assertNull("a dialog must never be attached to an activity that is going away",
            ShadowAlertDialog.getLatestAlertDialog());
    }
}
