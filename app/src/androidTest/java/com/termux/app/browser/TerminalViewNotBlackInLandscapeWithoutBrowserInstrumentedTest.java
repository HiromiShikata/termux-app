package com.termux.app.browser;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.RetryRule;
import com.termux.app.TermuxActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TerminalViewNotBlackInLandscapeWithoutBrowserInstrumentedTest {

    @Rule
    public final RetryRule retryRule = new RetryRule();

    @Test
    public void terminalViewHasMatchParentHeightAfterLandscapeRotationWhenBrowserIsHidden() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.reconfigureBrowserSplitForOrientation(true);

            View terminalView = activity.getTerminalView();
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) terminalView.getLayoutParams();
            assertEquals(LinearLayout.LayoutParams.MATCH_PARENT, params.height);
            assertEquals(0, params.width);
            assertEquals(1f, params.weight, 0f);
        });
    }

    @Test
    public void terminalViewRestoresWeightedHeightAfterRotatingBackToPortraitWhenBrowserIsHidden() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        scenario.onActivity(activity -> {
            TermuxBrowserController browserController = activity.getTermuxBrowserController();

            browserController.reconfigureBrowserSplitForOrientation(true);

            View terminalView = activity.getTerminalView();
            LinearLayout.LayoutParams paramsAfterLandscape =
                (LinearLayout.LayoutParams) terminalView.getLayoutParams();
            assertEquals(LinearLayout.LayoutParams.MATCH_PARENT, paramsAfterLandscape.height);

            browserController.reconfigureBrowserSplitForOrientation(false);

            LinearLayout.LayoutParams paramsAfterPortrait =
                (LinearLayout.LayoutParams) terminalView.getLayoutParams();
            assertEquals(LinearLayout.LayoutParams.MATCH_PARENT, paramsAfterPortrait.width);
            assertEquals(0, paramsAfterPortrait.height);
            assertEquals(1f, paramsAfterPortrait.weight, 0f);
        });
    }
}
