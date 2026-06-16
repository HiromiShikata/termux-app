package com.termux.app.terminal;

import android.widget.ImageButton;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionNavigationButtonsBinderTest {

    @Test
    public void previousSessionButtonRequestsPreviousSessionMatchingVolumeUp() {
        ImageButton previousSessionButton = new ImageButton(RuntimeEnvironment.getApplication());
        ImageButton nextSessionButton = new ImageButton(RuntimeEnvironment.getApplication());
        List<Boolean> directions = new ArrayList<>();

        SessionNavigationButtonsBinder.bind(previousSessionButton, nextSessionButton, directions::add);
        previousSessionButton.performClick();

        Assert.assertEquals(1, directions.size());
        Assert.assertEquals(Boolean.FALSE, directions.get(0));
    }

    @Test
    public void nextSessionButtonRequestsNextSessionMatchingVolumeDown() {
        ImageButton previousSessionButton = new ImageButton(RuntimeEnvironment.getApplication());
        ImageButton nextSessionButton = new ImageButton(RuntimeEnvironment.getApplication());
        List<Boolean> directions = new ArrayList<>();

        SessionNavigationButtonsBinder.bind(previousSessionButton, nextSessionButton, directions::add);
        nextSessionButton.performClick();

        Assert.assertEquals(1, directions.size());
        Assert.assertEquals(Boolean.TRUE, directions.get(0));
    }
}
