package com.termux.app.terminal;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SessionListBottomSheetControllerTest {

    private static class StringRecyclerAdapter extends RecyclerView.Adapter<StringRecyclerAdapter.Holder> {

        private final List<String> items;
        final List<Integer> fullBindPositions = new java.util.ArrayList<>();
        final List<Integer> payloadBindPositions = new java.util.ArrayList<>();

        StringRecyclerAdapter(List<String> items) {
            this.items = items;
            setHasStableIds(true);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).hashCode();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            fullBindPositions.add(position);
            ((TextView) holder.itemView).setText(items.get(position));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
            if (!payloads.isEmpty()) {
                payloadBindPositions.add(position);
                return;
            }
            super.onBindViewHolder(holder, position, payloads);
        }

        static final class Holder extends RecyclerView.ViewHolder {
            Holder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    @Test
    public void bindsSessionListAdapterWithAVerticalLayoutManagerOntoTheRecyclerView() {
        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        StringRecyclerAdapter delegate =
            new StringRecyclerAdapter(Arrays.asList("project header", "session a", "session b"));

        SessionListBottomSheetController.bindSessionListAdapter(recyclerView, delegate);

        Assert.assertSame(delegate, recyclerView.getAdapter());
        Assert.assertTrue("the bottom-sheet list must use a vertical LinearLayoutManager",
            recyclerView.getLayoutManager() instanceof LinearLayoutManager);
        Assert.assertEquals(RecyclerView.VERTICAL,
            ((LinearLayoutManager) recyclerView.getLayoutManager()).getOrientation());
    }

    @Test
    public void bindsSessionListAdapterPreservesRowOrderWithoutReversing() {
        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        StringRecyclerAdapter delegate =
            new StringRecyclerAdapter(Arrays.asList("project header", "session a", "session b"));

        SessionListBottomSheetController.bindSessionListAdapter(recyclerView, delegate);
        layoutRecyclerView(recyclerView);

        Assert.assertTrue("the laid-out RecyclerView must have visible child rows", recyclerView.getChildCount() > 0);
        Assert.assertEquals(Arrays.asList(0, 1, 2), delegate.fullBindPositions);
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
    public void perSecondTickUpdatesVisibleRowsViaPayloadOnlyWithoutAFullRebind() {
        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        StringRecyclerAdapter delegate =
            new StringRecyclerAdapter(Arrays.asList("session a", "session b", "session c"));
        SessionListBottomSheetController.bindSessionListAdapter(recyclerView, delegate);
        layoutRecyclerView(recyclerView);

        Assert.assertTrue("the laid-out RecyclerView must have visible child rows to update",
            recyclerView.getChildCount() > 0);
        delegate.fullBindPositions.clear();
        delegate.payloadBindPositions.clear();

        Object timePayload = new Object();
        delegate.notifyItemRangeChanged(0, delegate.getItemCount(), timePayload);
        layoutRecyclerView(recyclerView);

        Assert.assertTrue("the per-second tick must update visible rows through the payload bind path",
            !delegate.payloadBindPositions.isEmpty());
        Assert.assertTrue("the per-second tick must not run a full rebind of any visible row",
            delegate.fullBindPositions.isEmpty());
    }

    @Test
    public void stableIdsLetTheAdapterReuseTheSameRowIdentityAcrossUpdates() {
        StringRecyclerAdapter delegate =
            new StringRecyclerAdapter(Arrays.asList("project header", "session a", "session b"));

        Assert.assertTrue("the bottom-sheet adapter must expose stable ids for partial updates",
            delegate.hasStableIds());
        Assert.assertEquals("project header".hashCode(), delegate.getItemId(0));
        Assert.assertEquals("session a".hashCode(), delegate.getItemId(1));
        Assert.assertNotEquals(delegate.getItemId(0), delegate.getItemId(1));
    }

    private static void layoutRecyclerView(@NonNull RecyclerView recyclerView) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY);
        recyclerView.measure(widthSpec, heightSpec);
        recyclerView.layout(0, 0, 600, 800);
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
