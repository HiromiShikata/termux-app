package com.termux.app.activities;

import android.content.ClipboardManager;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.PreferenceViewHolder;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LongClickCopyPreferenceTest {

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxApp_DayNight_NoActionBar);
    }

    private View bindAndGetItemView(LongClickCopyPreference preference, Context context) {
        View itemView = new View(context);
        new FrameLayout(context).addView(itemView);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(itemView);
        preference.onBindViewHolder(holder);
        return itemView;
    }

    @Test
    public void longClickCopiesConfiguredTextAndConsumesEvent() {
        Context context = themedContext();
        LongClickCopyPreference preference = new LongClickCopyPreference(context, null);
        preference.setCopyText("0.118.0 (118)");
        preference.setCopyConfirmationToast("Version copied");

        View itemView = bindAndGetItemView(preference, context);
        boolean consumed = itemView.performLongClick();

        Assert.assertTrue(consumed);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        Assert.assertNotNull(clipboard.getPrimaryClip());
        Assert.assertEquals("0.118.0 (118)",
            clipboard.getPrimaryClip().getItemAt(0).getText().toString());
    }

    @Test
    public void longClickWithoutCopyTextDoesNotConsumeEvent() {
        Context context = themedContext();
        LongClickCopyPreference preference = new LongClickCopyPreference(context, null);

        View itemView = bindAndGetItemView(preference, context);
        boolean consumed = itemView.performLongClick();

        Assert.assertFalse(consumed);
    }
}
