package com.termux.app.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.widget.EditText;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class BrowserJsDialogTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private JsResult newJsResult() {
        return ReflectionHelpers.callConstructor(JsResult.class);
    }

    private JsPromptResult newJsPromptResult() {
        return ReflectionHelpers.callConstructor(JsPromptResult.class);
    }

    @Test
    public void showAlertDisplaysMessage() {
        BrowserJsDialog.showAlert(context(), "Alert message", newJsResult());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(dialog);
        Assert.assertTrue(dialog.isShowing());
        ShadowAlertDialog shadow = org.robolectric.Shadows.shadowOf(dialog);
        Assert.assertEquals("Alert message", shadow.getMessage());
    }

    @Test
    public void showConfirmDisplaysMessage() {
        BrowserJsDialog.showConfirm(context(), "Confirm message", newJsResult());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(dialog);
        ShadowAlertDialog shadow = org.robolectric.Shadows.shadowOf(dialog);
        Assert.assertEquals("Confirm message", shadow.getMessage());
    }

    @Test
    public void showPromptPrefillsDefaultValueAndMessage() {
        BrowserJsDialog.showPrompt(context(), "Prompt message", "default value", newJsPromptResult());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(dialog);
        TextView messageView = dialog.findViewById(R.id.browser_js_prompt_message);
        Assert.assertNotNull(messageView);
        Assert.assertEquals("Prompt message", messageView.getText().toString());
        EditText valueInput = dialog.findViewById(R.id.browser_js_prompt_value);
        Assert.assertNotNull(valueInput);
        Assert.assertEquals("default value", valueInput.getText().toString());
    }

    @Test
    public void confirmingAlertDoesNotThrow() {
        BrowserJsDialog.showAlert(context(), "Alert", newJsResult());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }

    @Test
    public void cancellingConfirmDoesNotThrow() {
        BrowserJsDialog.showConfirm(context(), "Confirm", newJsResult());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
    }

    @Test
    public void confirmingPromptDoesNotThrow() {
        BrowserJsDialog.showPrompt(context(), "Prompt", "value", newJsPromptResult());
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }
}
