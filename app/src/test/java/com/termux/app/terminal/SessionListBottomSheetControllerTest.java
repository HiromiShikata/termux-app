package com.termux.app.terminal;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionListBottomSheetControllerTest {

    private static class StringListAdapter extends BaseAdapter {

        private final List<String> items;

        StringListAdapter(List<String> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView textView = new TextView(parent.getContext());
            textView.setText(items.get(position));
            return textView;
        }
    }

    @Test
    public void bindsSessionListInSameOrderAsLeftDrawerWithoutReversing() {
        ListView listView = new ListView(RuntimeEnvironment.getApplication());
        StringListAdapter delegate =
            new StringListAdapter(Arrays.asList("project header", "session a", "session b"));

        SessionListBottomSheetController.bindSessionListAdapter(listView, delegate);

        Assert.assertSame(delegate, listView.getAdapter());
        Assert.assertEquals("project header", listView.getAdapter().getItem(0));
        Assert.assertEquals("session a", listView.getAdapter().getItem(1));
        Assert.assertEquals("session b", listView.getAdapter().getItem(2));
    }

    @Test
    public void togglingFromHiddenOpensSheet() {
        Assert.assertEquals(View.VISIBLE, SessionListBottomSheetController.nextSheetVisibility(View.GONE));
    }

    @Test
    public void togglingFromVisibleClosesSheet() {
        Assert.assertEquals(View.GONE, SessionListBottomSheetController.nextSheetVisibility(View.VISIBLE));
    }

    @Test
    public void scrimIsVisibleWhileSheetIsOpenSoOutsideTapsCanDismissIt() {
        Assert.assertEquals(View.VISIBLE, SessionListBottomSheetController.scrimVisibilityForSheet(View.VISIBLE));
    }

    @Test
    public void scrimIsGoneWhileSheetIsClosedSoItDoesNotBlockInteraction() {
        Assert.assertEquals(View.GONE, SessionListBottomSheetController.scrimVisibilityForSheet(View.GONE));
    }

    @Test
    public void sessionInfoIsHiddenWhileSheetIsOpenSoItDoesNotShowBeneathTheSheet() {
        Assert.assertEquals(View.INVISIBLE, SessionListBottomSheetController.sessionInfoVisibilityForSheet(View.VISIBLE));
    }

    @Test
    public void sessionInfoIsVisibleWhileSheetIsClosedSoItShowsAboveTheButtonArea() {
        Assert.assertEquals(View.VISIBLE, SessionListBottomSheetController.sessionInfoVisibilityForSheet(View.GONE));
    }

    @Test
    public void hideIfPresentDoesNothingWhenControllerIsNull() {
        SessionListBottomSheetController.hideIfPresent(null);
    }

    @Test
    public void followCurrentSessionRevealsRowWhenSheetIsShowing() {
        Assert.assertTrue(SessionListBottomSheetController.shouldRevealForVisibility(View.VISIBLE));
    }

    @Test
    public void followCurrentSessionDoesNothingWhenSheetIsHidden() {
        Assert.assertFalse(SessionListBottomSheetController.shouldRevealForVisibility(View.GONE));
        Assert.assertFalse(SessionListBottomSheetController.shouldRevealForVisibility(View.INVISIBLE));
    }

    @Test
    public void openingSheetClosesTheBrowserWhenTheBrowserIsCurrentlyShowing() {
        Assert.assertTrue(SessionListBottomSheetController.shouldHideBrowserOnOpen(true));
    }

    @Test
    public void openingSheetLeavesTheTerminalUntouchedWhenTheBrowserIsNotShowing() {
        Assert.assertFalse(SessionListBottomSheetController.shouldHideBrowserOnOpen(false));
    }

    @Test
    public void relativeTimeRefreshKeepsTickingWhileTheSheetIsVisible() {
        Assert.assertTrue(SessionListBottomSheetController.shouldKeepRefreshing(View.VISIBLE));
    }

    @Test
    public void relativeTimeRefreshStopsWhenTheSheetIsHidden() {
        Assert.assertFalse(SessionListBottomSheetController.shouldKeepRefreshing(View.GONE));
        Assert.assertFalse(SessionListBottomSheetController.shouldKeepRefreshing(View.INVISIBLE));
    }

    @Test
    public void relativeTimeRefreshIntervalIsAboutOneSecond() {
        Assert.assertEquals(1000L,
            SessionListBottomSheetController.RELATIVE_TIME_REFRESH_INTERVAL_MILLISECONDS);
    }

    @Test
    public void relativeTimeRefreshRunsWhileNoSessionRowTapIsInProgress() {
        Assert.assertTrue(SessionListBottomSheetController.shouldRefreshNow(false));
    }

    @Test
    public void relativeTimeRefreshIsSuppressedWhileASessionRowTapIsInProgressSoTheFirstTapNavigates() {
        Assert.assertFalse(SessionListBottomSheetController.shouldRefreshNow(true));
    }

    @Test
    public void sessionRowTouchInteractionEndsOnPointerUpOrCancelSoTheDeferredRefreshCanResume() {
        Assert.assertTrue(SessionListBottomSheetController.isSessionListTouchInteractionEnd(MotionEvent.ACTION_UP));
        Assert.assertTrue(SessionListBottomSheetController.isSessionListTouchInteractionEnd(MotionEvent.ACTION_CANCEL));
        Assert.assertFalse(SessionListBottomSheetController.isSessionListTouchInteractionEnd(MotionEvent.ACTION_DOWN));
        Assert.assertFalse(SessionListBottomSheetController.isSessionListTouchInteractionEnd(MotionEvent.ACTION_MOVE));
    }

    @Test
    public void aSingleTapOnASessionRowNavigatesOnTheFirstTapBecauseNoRefreshRebindsTheRowDuringTheTouch() {
        TouchGatedRefreshHarness harness = new TouchGatedRefreshHarness();
        int[] navigatedPosition = {-1};

        harness.onTouch(MotionEvent.ACTION_DOWN);
        harness.requestPeriodicRefreshTick();
        harness.onTouch(MotionEvent.ACTION_UP);
        navigatedPosition[0] = 1;

        Assert.assertEquals("no notifyDataSetChanged may run between ACTION_DOWN and ACTION_UP, so AbsListView keeps the pending item click",
            0, harness.refreshCountDuringTouch);
        Assert.assertEquals("the first tap must navigate to the tapped session", 1, navigatedPosition[0]);
    }

    @Test
    public void aPeriodicRefreshRequestedDuringATapIsDeferredUntilTheTapEndsThenRunsOnce() {
        TouchGatedRefreshHarness harness = new TouchGatedRefreshHarness();

        harness.onTouch(MotionEvent.ACTION_DOWN);
        harness.requestPeriodicRefreshTick();

        Assert.assertEquals("the refresh requested mid-tap must not run while the finger is down",
            0, harness.totalRefreshCount);

        harness.onTouch(MotionEvent.ACTION_UP);

        Assert.assertEquals("the deferred refresh must run exactly once after the tap ends",
            1, harness.totalRefreshCount);
    }

    @Test
    public void periodicRefreshTicksNormallyWhenNoTapIsInProgress() {
        TouchGatedRefreshHarness harness = new TouchGatedRefreshHarness();

        harness.requestPeriodicRefreshTick();
        harness.requestPeriodicRefreshTick();

        Assert.assertEquals("with no touch in progress, every tick refreshes immediately",
            2, harness.totalRefreshCount);
    }

    private static final class TouchGatedRefreshHarness {
        private boolean touchInProgress;
        private boolean refreshDeferredByTouch;
        int totalRefreshCount;
        int refreshCountDuringTouch;

        void onTouch(int maskedAction) {
            if (maskedAction == MotionEvent.ACTION_DOWN) {
                touchInProgress = true;
            } else if (SessionListBottomSheetController.isSessionListTouchInteractionEnd(maskedAction)) {
                touchInProgress = false;
                if (refreshDeferredByTouch) {
                    refreshDeferredByTouch = false;
                    runRefresh();
                }
            }
        }

        void requestPeriodicRefreshTick() {
            if (!SessionListBottomSheetController.shouldRefreshNow(touchInProgress)) {
                refreshDeferredByTouch = true;
                return;
            }
            runRefresh();
        }

        private void runRefresh() {
            totalRefreshCount++;
            if (touchInProgress) {
                refreshCountDuringTouch++;
            }
        }
    }
}
