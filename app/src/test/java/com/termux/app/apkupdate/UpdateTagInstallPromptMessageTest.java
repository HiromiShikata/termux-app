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
public class UpdateTagInstallPromptMessageTest {

    @Test
    public void updatePromptMessageContainsVersionAndReason() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        String reason = "セキュリティ修正のため更新してください";
        String message = activity.getString(R.string.update_tag_update_dialog_message, "1.2.3", reason);

        DialogUtils.showDismissibleOnTouchOutside(new AlertDialog.Builder(activity)
            .setTitle(R.string.apk_update_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.apk_update_dialog_install, null)
            .setNegativeButton(R.string.apk_update_dialog_cancel, null));

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Assert.assertNotNull(dialog);
        Assert.assertTrue(dialog.isShowing());

        TextView messageView = dialog.findViewById(android.R.id.message);
        Assert.assertNotNull(messageView);
        String shownText = messageView.getText().toString();
        Assert.assertTrue(shownText.contains("1.2.3"));
        Assert.assertTrue(shownText.contains(reason));
    }
}
