package com.termux.shared.logger;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
public class LoggerShowToastTest {

    @Test
    public void createTopGravityToastRequestsTopPlacement() {
        Context context = RuntimeEnvironment.getApplication();

        Toast toast = Logger.createTopGravityToast(context, "Text copied to clipboard", false);

        Assert.assertEquals(Gravity.TOP, toast.getGravity());
        Assert.assertEquals(0, toast.getXOffset());
        Assert.assertEquals(0, toast.getYOffset());
    }

    @Test
    public void showToastDisplaysAtTopOfScreen() {
        Context context = RuntimeEnvironment.getApplication();

        Logger.showToast(context, "Session name copied", false);
        shadowOf(Looper.getMainLooper()).idle();

        Toast latestToast = ShadowToast.getLatestToast();
        Assert.assertNotNull(latestToast);
        Assert.assertEquals(Gravity.TOP, latestToast.getGravity());
    }
}
