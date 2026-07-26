package com.termux.app.browser;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reproduction test for issue #1466: the in-app browser's loading indicator
 * is global rather than per-tab. TermuxBrowserController's single shared
 * ProgressBar/SwipeRefreshLayout are already gated by isDisplayedTab(tab), so
 * they correctly reflect only the currently displayed tab. The actual defect
 * is that the favicon tab strip - which renders every open tab at once via
 * BrowserTabFaviconStripController.update() - has no mechanism at all to show
 * which tab(s) are loading: BrowserTab (prior to this change) had no loading
 * flag, and update() never consulted one. As a result, a background tab that
 * is loading gives the user no visual feedback in the strip, and there is no
 * way to distinguish a loading tab from an idle one by looking at the strip.
 *
 * This test asserts that the rendered strip item for a loading tab must be
 * visually distinguishable (some descendant view's visibility must differ)
 * from the rendered strip item for an idle tab. It fails on unfixed main
 * because update() currently produces identical view trees for both tabs
 * regardless of BrowserTab#isLoading().
 */
@RunWith(RobolectricTestRunner.class)
public class BrowserTabFaviconStripLoadingIndicatorTest {

    private static final String SESSION = "session";

    private static final class RecordingSelectionListener implements BrowserTabSelectionListener {
        @Nullable BrowserTab activeTab;

        @Override
        public void openTab(@NonNull BrowserTab tab) {
        }

        @Override
        public void closeTab(@NonNull BrowserTab tab) {
        }

        @Override
        public void promptNewTab() {
        }

        @Nullable
        @Override
        public BrowserTab getActiveTab() {
            return activeTab;
        }
    }

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    @Test
    public void onlyTheLoadingTabsStripItemIsVisuallyDistinguishedAsLoading() {
        Context context = themedContext();
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout container = new LinearLayout(context);
        RecordingSelectionListener listener = new RecordingSelectionListener();
        // Neither tab is the active tab, so the existing active-indicator
        // view cannot be mistaken for a loading indicator in this test.
        listener.activeTab = null;

        BrowserTab loadingTab = new BrowserTab(SESSION, "https://loading.example/");
        BrowserTab idleTab = new BrowserTab(SESSION, "https://idle.example/");
        loadingTab.setLoading(true);
        idleTab.setLoading(false);

        new BrowserTabFaviconStripController(scrollView, container, listener)
            .update(Arrays.asList(loadingTab, idleTab), null);

        View loadingTabItem = container.getChildAt(0);
        View idleTabItem = container.getChildAt(1);

        List<Integer> loadingTabVisibilities = collectDescendantVisibilities(loadingTabItem);
        List<Integer> idleTabVisibilities = collectDescendantVisibilities(idleTabItem);

        Assert.assertNotEquals(
            "The favicon tab strip must render a visually distinct loading "
                + "indicator for a tab that is loading versus a tab that is "
                + "not; on unfixed main BrowserTab#isLoading() is never "
                + "consulted by BrowserTabFaviconStripController#update(), so "
                + "the loading tab's strip item and the idle tab's strip item "
                + "render with identical descendant visibilities.",
            loadingTabVisibilities, idleTabVisibilities);
    }

    @NonNull
    private List<Integer> collectDescendantVisibilities(@NonNull View root) {
        List<Integer> visibilities = new ArrayList<>();
        visibilities.add(root.getVisibility());
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                visibilities.addAll(collectDescendantVisibilities(group.getChildAt(i)));
            }
        }
        return visibilities;
    }
}
