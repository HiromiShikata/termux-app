package com.termux.app.apkupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;

import com.termux.R;
import com.termux.shared.interact.DialogUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
public class UpdateDialogContentTextSizeReducerTest {

    private AlertDialog showUpdateDialog(Activity activity) {
        String message = activity.getString(R.string.update_tag_install_dialog_message, "1.2.3", "reason");
        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.update_tag_install_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.apk_update_dialog_install, null)
            .setNegativeButton(R.string.apk_update_dialog_cancel, null));
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    @Test
    public void reduceContentTextToHalfHalvesTheMessageTextSize() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();

        AlertDialog dialog = showUpdateDialog(activity);
        TextView messageView = dialog.findViewById(android.R.id.message);
        float defaultSize = messageView.getTextSize();
        Assert.assertTrue(defaultSize > 0f);

        UpdateDialogContentTextSizeReducer.reduceContentTextToHalf(dialog);

        Assert.assertEquals(defaultSize * 0.5f, messageView.getTextSize(), 0.01f);
    }

    @Test
    public void reduceContentTextToHalfToleratesNullDialog() {
        UpdateDialogContentTextSizeReducer.reduceContentTextToHalf(null);
    }
}
