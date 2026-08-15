package com.termux.app.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserNewTabDialogCopiedLinkButtonTest {

    private static final String COPIED_ADDRESS = "https://example.com/a/page?q=1";

    private static Activity newActivity() {
        return Robolectric.buildActivity(Activity.class).setup().get();
    }

    private static View inflateDialogContent(Activity activity) {
        return LayoutInflater.from(activity).inflate(R.layout.dialog_browser_new_tab, null);
    }

    private static AlertDialog shownDialogFor(Activity activity, String clipboardText,
                                              List<String> openedUrls) {
        AlertDialog dialog = BrowserNewTabDialog.create(activity, inflateDialogContent(activity),
            BrowserClipboardUrlOffer.of(clipboardText), openedUrls::add);
        dialog.show();
        return dialog;
    }

    @Test
    public void theCopiedLinkIsOfferedFromTheButtonAreaAndNamesWhereAPressWouldGo() {
        Activity activity = newActivity();

        AlertDialog dialog = shownDialogFor(activity, COPIED_ADDRESS, new ArrayList<>());
        Button copiedLinkButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

        Assert.assertNotNull("the owner asked for this control to sit in the button area alongside the"
            + " button that opens what he typed and the one that cancels", copiedLinkButton);
        Assert.assertEquals("a control the button area does not draw cannot be pressed",
            View.VISIBLE, copiedLinkButton.getVisibility());
        Assert.assertTrue("the clipboard holds whatever was last copied anywhere on the device, so the"
                + " owner has to see where a press would take him before pressing. Actual: "
                + copiedLinkButton.getText(),
            copiedLinkButton.getText().toString().contains("example.com"));
    }

    @Test
    public void pressingTheCopiedLinkOpensTheAddressThatWasCopiedAndClosesTheDialog() {
        Activity activity = newActivity();
        List<String> openedUrls = new ArrayList<>();

        AlertDialog dialog = shownDialogFor(activity, COPIED_ADDRESS, openedUrls);
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals("the press has to open the address that was copied, whole, not the site it"
                + " names on the button", java.util.Collections.singletonList(COPIED_ADDRESS), openedUrls);
        Assert.assertFalse("the dialog has to close when the copied address opens, as it does for a"
            + " typed address and for a list entry", dialog.isShowing());
    }

    @Test
    public void theButtonAreaCarriesOnlyOpenAndCancelWhenNothingAddressableWasCopied() {
        Activity activity = newActivity();

        AlertDialog dialog = shownDialogFor(activity, "the meeting is at four", new ArrayList<>());
        Button copiedLinkButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

        Assert.assertTrue("a third button that opens nothing would take room from the two the dialog"
                + " needs",
            copiedLinkButton == null || copiedLinkButton.getVisibility() != View.VISIBLE);
    }

    @Test
    public void theDialogContentPutsTheAddressFieldFirstAndAddsNoButtonAboveIt() {
        Activity activity = newActivity();

        ViewGroup dialogContent = (ViewGroup) inflateDialogContent(activity);

        Assert.assertEquals("the owner opens this dialog to type an address, and he reported that a"
                + " control above that field gets in the way",
            R.id.browser_new_tab_url_input, dialogContent.getChildAt(0).getId());
        Assert.assertEquals("a button placed in the dialog's own content sits above the address field"
                + " and pushes it and the list below it down", 0, buttonCount(dialogContent));
    }

    private static int buttonCount(ViewGroup group) {
        int count = 0;
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child instanceof Button) count++;
            if (child instanceof ViewGroup) count += buttonCount((ViewGroup) child);
        }
        return count;
    }
}
