package com.termux.shared.termux.interact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Looper;
import android.widget.EditText;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class TextInputDialogUtilsTest {

    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.setTheme(android.R.style.Theme_Material_Light);
    }

    private void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private AlertDialog latestDialog() {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull("a dialog must have been shown", dialog);
        Assert.assertTrue("dialog must be visible", dialog.isShowing());
        return dialog;
    }

    private EditText findEditText(AlertDialog dialog) {
        // The custom view layout is attached under the dialog; traverse from the decor view.
        return findEditTextInView(dialog.getWindow().getDecorView());
    }

    private EditText findEditTextInView(android.view.View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findEditTextInView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    public void textInputShowsDialogWithInitialText() {
        TextInputDialogUtils.textInput(activity, android.R.string.dialog_alert_title, "preset-value",
            android.R.string.ok, text -> { },
            0, null,
            0, null,
            null);

        AlertDialog dialog = latestDialog();
        EditText input = findEditText(dialog);
        Assert.assertNotNull(input);
        Assert.assertEquals("preset-value", input.getText().toString());
    }

    @Test
    public void textInputPositiveButtonReportsCurrentText() {
        AtomicReference<String> captured = new AtomicReference<>(null);
        TextInputDialogUtils.textInput(activity, android.R.string.dialog_alert_title, "hello",
            android.R.string.ok, captured::set,
            0, null,
            0, null,
            null);

        AlertDialog dialog = latestDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        idle();
        Assert.assertEquals("hello", captured.get());
    }

    @Test
    public void textInputNeutralButtonReportsCurrentText() {
        AtomicReference<String> captured = new AtomicReference<>(null);
        TextInputDialogUtils.textInput(activity, android.R.string.dialog_alert_title, "neutral-text",
            android.R.string.ok, text -> { },
            android.R.string.untitled, captured::set,
            0, null,
            null);

        AlertDialog dialog = latestDialog();
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick();
        idle();
        Assert.assertEquals("neutral-text", captured.get());
    }

    @Test
    public void textInputNegativeButtonReportsCurrentText() {
        AtomicReference<String> captured = new AtomicReference<>(null);
        TextInputDialogUtils.textInput(activity, android.R.string.dialog_alert_title, "negative-text",
            android.R.string.ok, text -> { },
            0, null,
            android.R.string.cancel, captured::set,
            null);

        AlertDialog dialog = latestDialog();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        idle();
        Assert.assertEquals("negative-text", captured.get());
    }

    @Test
    public void textInputInvokesDismissListener() {
        AtomicBoolean dismissed = new AtomicBoolean(false);
        TextInputDialogUtils.textInput(activity, android.R.string.dialog_alert_title, null,
            android.R.string.ok, text -> { },
            0, null,
            0, null,
            d -> dismissed.set(true));

        AlertDialog dialog = latestDialog();
        dialog.dismiss();
        idle();
        Assert.assertTrue(dismissed.get());
    }

    @Test
    public void twoTextInputsShowsDialogAndPositiveButtonReportsBothTexts() {
        AtomicReference<String> first = new AtomicReference<>(null);
        AtomicReference<String> second = new AtomicReference<>(null);
        TextInputDialogUtils.twoTextInputs(activity, android.R.string.dialog_alert_title,
            android.R.string.untitled, android.R.string.unknownName,
            android.R.string.ok,
            (text1, text2) -> { first.set(text1); second.set(text2); },
            null);

        AlertDialog dialog = latestDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        idle();
        Assert.assertEquals("", first.get());
        Assert.assertEquals("", second.get());
    }

    @Test
    public void twoTextInputsInvokesDismissListener() {
        AtomicBoolean dismissed = new AtomicBoolean(false);
        TextInputDialogUtils.twoTextInputs(activity, android.R.string.dialog_alert_title,
            android.R.string.untitled, android.R.string.unknownName,
            android.R.string.ok,
            (text1, text2) -> { },
            d -> dismissed.set(true));

        AlertDialog dialog = latestDialog();
        dialog.dismiss();
        idle();
        Assert.assertTrue(dismissed.get());
    }
}
