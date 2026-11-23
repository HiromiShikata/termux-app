package com.termux.app.handedness;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VoiceConsoleButtonPlacementTest {

    @Test
    public void voiceConsoleButtonIsNotInsideTheScrollableIconRow() {
        View root = inflateActivityLayout();
        View voiceConsoleButton = root.findViewById(R.id.terminal_toolbar_voice_console_button);
        View iconButtonScroll = root.findViewById(R.id.terminal_toolbar_icon_button_scroll);

        Assert.assertFalse(isDescendantOf(voiceConsoleButton, iconButtonScroll));
    }

    @Test
    public void voiceConsoleButtonIsInTheSameVerticalLeftColumnAboveTheHandednessSwitch() {
        View root = inflateActivityLayout();
        View voiceConsoleButton = root.findViewById(R.id.terminal_toolbar_voice_console_button);
        View handednessSwitchButton = root.findViewById(R.id.terminal_toolbar_handedness_switch_button);
        LinearLayout leftColumn = (LinearLayout) handednessSwitchButton.getParent();

        Assert.assertSame(leftColumn, voiceConsoleButton.getParent());
        Assert.assertEquals(LinearLayout.VERTICAL, leftColumn.getOrientation());
        Assert.assertSame(root.findViewById(R.id.terminal_toolbar_bottom_container), leftColumn.getParent());
        Assert.assertEquals(0, leftColumn.indexOfChild(voiceConsoleButton));
        Assert.assertEquals(1, leftColumn.indexOfChild(handednessSwitchButton));
    }

    private static boolean isDescendantOf(View view, View ancestor) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == ancestor) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }

}
