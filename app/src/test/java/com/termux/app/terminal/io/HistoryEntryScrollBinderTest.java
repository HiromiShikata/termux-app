package com.termux.app.terminal.io;

import android.content.Context;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;
import com.termux.app.terminal.MaxHeightScrollView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HistoryEntryScrollBinderTest {

    private static final int WIDTH_PIXELS = 400;

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private View inflateRow(Context context) {
        return LayoutInflater.from(context)
            .inflate(R.layout.item_toolbar_text_input_history, new FrameLayout(context), false);
    }

    private void measure(View view) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    @Test
    public void boundsTheEntryToThreeLinesOfItsOwnLineHeight() {
        Context context = themedContext();
        View row = inflateRow(context);
        TextView entryView = row.findViewById(R.id.toolbar_text_input_history_entry);
        MaxHeightScrollView entryScroll =
            row.findViewById(R.id.toolbar_text_input_history_entry_scroll);
        entryView.setText("single line");

        HistoryEntryScrollBinder.bind(entryScroll, entryView);

        Assert.assertEquals(
            "the entry must be capped to exactly three of its rendered text lines",
            entryView.getLineHeight() * HistoryEntryScrollBinder.MAX_VISIBLE_LINES,
            entryScroll.getMaxScrollHeightPixels());
    }

    @Test
    public void longEntryStaysCappedToThreeLinesAndScrollsToRevealTheRest() {
        Context context = themedContext();
        View row = inflateRow(context);
        TextView entryView = row.findViewById(R.id.toolbar_text_input_history_entry);
        MaxHeightScrollView entryScroll =
            row.findViewById(R.id.toolbar_text_input_history_entry_scroll);
        entryView.setText("a long pinned prompt");

        HistoryEntryScrollBinder.bind(entryScroll, entryView);
        int cap = entryScroll.getMaxScrollHeightPixels();
        entryView.setMinimumHeight(cap * 6);
        measure(row);

        Assert.assertEquals("a long entry must not grow past the three-line cap",
            cap, entryScroll.getMeasuredHeight());
        Assert.assertTrue("the full text must remain present so it can be scrolled into view",
            entryScroll.getChildAt(0).getMeasuredHeight() > cap);
    }

    @Test
    public void shortEntryKeepsItsNaturalHeightAndDoesNotScroll() {
        Context context = themedContext();
        View row = inflateRow(context);
        TextView entryView = row.findViewById(R.id.toolbar_text_input_history_entry);
        MaxHeightScrollView entryScroll =
            row.findViewById(R.id.toolbar_text_input_history_entry_scroll);
        entryView.setText("ls");

        HistoryEntryScrollBinder.bind(entryScroll, entryView);
        measure(row);

        int cap = entryScroll.getMaxScrollHeightPixels();
        Assert.assertTrue("a short entry must stay below the three-line cap",
            entryScroll.getMeasuredHeight() <= cap);
        Assert.assertFalse("a short entry must not be internally scrollable",
            entryScroll.canScrollVertically(1) || entryScroll.canScrollVertically(-1));
    }

    @Test
    public void resetsScrollPositionWhenTheRowIsRebound() {
        Context context = themedContext();
        View row = inflateRow(context);
        TextView entryView = row.findViewById(R.id.toolbar_text_input_history_entry);
        MaxHeightScrollView entryScroll =
            row.findViewById(R.id.toolbar_text_input_history_entry_scroll);
        entryView.setText("first");
        HistoryEntryScrollBinder.bind(entryScroll, entryView);
        entryScroll.scrollTo(0, 999);

        entryView.setText("second");
        HistoryEntryScrollBinder.bind(entryScroll, entryView);

        Assert.assertEquals("a recycled row must start scrolled to the top of its entry",
            0, entryScroll.getScrollY());
    }

    @Test
    public void overflowingEntryLetsTheInnerScrollWinAgainstTheDialogList() {
        Context context = themedContext();
        RecordingParent parent = new RecordingParent(context);
        MaxHeightScrollView entryScroll = new MaxHeightScrollView(context);
        TextView entryView = new TextView(context);
        entryScroll.addView(entryView,
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(entryScroll);
        entryView.setText("overflowing prompt");

        HistoryEntryScrollBinder.bind(entryScroll, entryView);
        entryView.setMinimumHeight(entryScroll.getMaxScrollHeightPixels() * 6);
        measure(parent);

        entryScroll.dispatchTouchEvent(motionEvent(MotionEvent.ACTION_DOWN));
        Assert.assertEquals(Boolean.TRUE, parent.lastDisallowInterceptRequest);

        entryScroll.dispatchTouchEvent(motionEvent(MotionEvent.ACTION_UP));
        Assert.assertEquals(Boolean.FALSE, parent.lastDisallowInterceptRequest);
    }

    @Test
    public void shortEntryDoesNotStealTouchesFromTheDialogList() {
        Context context = themedContext();
        RecordingParent parent = new RecordingParent(context);
        MaxHeightScrollView entryScroll = new MaxHeightScrollView(context);
        TextView entryView = new TextView(context);
        entryScroll.addView(entryView,
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(entryScroll);
        entryView.setText("ls");

        HistoryEntryScrollBinder.bind(entryScroll, entryView);
        measure(parent);

        entryScroll.dispatchTouchEvent(motionEvent(MotionEvent.ACTION_DOWN));

        Assert.assertNull("a short entry must let the dialog handle the tap as a selection",
            parent.lastDisallowInterceptRequest);
    }

    private MotionEvent motionEvent(int action) {
        long time = SystemClock.uptimeMillis();
        return MotionEvent.obtain(time, time, action, 5f, 5f, 0);
    }

    private static final class RecordingParent extends FrameLayout {

        private Boolean lastDisallowInterceptRequest;

        RecordingParent(Context context) {
            super(context);
        }

        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            lastDisallowInterceptRequest = disallowIntercept;
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }
}
