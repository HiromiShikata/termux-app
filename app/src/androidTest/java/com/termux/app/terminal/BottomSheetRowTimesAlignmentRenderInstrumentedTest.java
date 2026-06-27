package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BottomSheetRowTimesAlignmentRenderInstrumentedTest {

    private static final int GROUPED_INDENT_DP = 24;
    private static final int VERTICAL_PADDING_DP = 6;
    private static final int BELL_ICON_WIDTH_DP = 16;
    private static final int BELL_ICON_PADDING_DP = 4;

    @Test
    public void timesRowLeftEdgeAlignsWithTitleTextLeftEdgeForGroupedRows() {
        Context baseContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context context = new ContextThemeWrapper(baseContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionNewActivityStore store = new SessionNewActivityStore();
        String sessionName = "claude-build-worker";
        String timestamp = TermuxSessionsListViewController.buildTimestampLine(store, sessionName, 61_000L);

        SessionInfoBlock titleBlock = SessionInfoBlock.compose("", sessionName, "",
            "build automation project", "claude session subtitle", "");

        AtomicReference<int[]> titleTextLeftAndTimesLeft = new AtomicReference<>(new int[]{-1, -1});
        AtomicReference<String> screenshotBase64 = new AtomicReference<>("");
        AtomicInteger renderedWidth = new AtomicInteger();
        AtomicInteger renderedHeight = new AtomicInteger();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            int verticalPadding = dpToPx(context, VERTICAL_PADDING_DP);
            int titleStartPadding = dpToPx(context, GROUPED_INDENT_DP);
            int timesStartPadding = TermuxSessionsListViewController.sessionRowTimesStartPaddingPx(
                titleStartPadding, dpToPx(context, BELL_ICON_WIDTH_DP), dpToPx(context, BELL_ICON_PADDING_DP));

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setBackgroundColor(0xFF202124);

            int previousFixedTimesStartPadding = dpToPx(context, VERTICAL_PADDING_DP);
            container.addView(buildGroupLabel(context, "BEFORE (times protrude left)"));
            container.addView(buildRow(context, titleBlock.text(), timestamp,
                titleStartPadding, verticalPadding, previousFixedTimesStartPadding));

            container.addView(buildGroupLabel(context, "AFTER (times aligned with title text)"));
            View row = buildRow(context, titleBlock.text(), timestamp,
                titleStartPadding, verticalPadding, timesStartPadding);
            TextView title = row.findViewById(R.id.session_title);
            TextView times = row.findViewById(R.id.session_row_times);
            assertNotNull(title);
            assertNotNull(times);
            container.addView(row);

            float density = context.getResources().getDisplayMetrics().density;
            int widthPx = (int) (320 * density);
            int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            container.measure(widthSpec, heightSpec);
            container.layout(0, 0, container.getMeasuredWidth(), container.getMeasuredHeight());

            int titleTextLeft = title.getLeft() + title.getTotalPaddingStart();
            int timesTextLeft = times.getLeft() + times.getTotalPaddingStart();
            titleTextLeftAndTimesLeft.set(new int[]{titleTextLeft, timesTextLeft});

            renderedWidth.set(container.getMeasuredWidth());
            renderedHeight.set(container.getMeasuredHeight());
            screenshotBase64.set(encodeBitmap(container));
        });

        int titleTextLeft = titleTextLeftAndTimesLeft.get()[0];
        int timesTextLeft = titleTextLeftAndTimesLeft.get()[1];
        System.out.println("TIMES_ALIGN titleTextLeftPx=" + titleTextLeft
            + " timesTextLeftPx=" + timesTextLeft);
        System.out.println("TIMES_ALIGN renderedWidthPx=" + renderedWidth.get()
            + " renderedHeightPx=" + renderedHeight.get());

        String base64 = screenshotBase64.get();
        int chunk = 2000;
        for (int i = 0; i < base64.length(); i += chunk) {
            System.out.println("ALIGN_SHOT_B64 " + base64.substring(i, Math.min(base64.length(), i + chunk)));
        }
        System.out.println("ALIGN_SHOT_B64_END");

        assertTrue("the call/out/reply times row left edge must align with the session title text left edge; "
                + "titleTextLeftPx=" + titleTextLeft + " timesTextLeftPx=" + timesTextLeft,
            Math.abs(titleTextLeft - timesTextLeft) <= 1);
    }

    private static View buildRow(Context context, CharSequence titleText, String timestamp,
                                 int titleStartPadding, int verticalPadding, int timesStartPadding) {
        View row = LayoutInflater.from(context)
            .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);
        TextView title = row.findViewById(R.id.session_title);
        TextView times = row.findViewById(R.id.session_row_times);
        assertNotNull(title);
        assertNotNull(times);

        title.setPadding(titleStartPadding, verticalPadding, verticalPadding, verticalPadding);
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_session_activity_dot_gray, 0, 0, 0);
        title.setCompoundDrawablePadding(dpToPx(context, BELL_ICON_PADDING_DP));
        title.setTextColor(0xFFE8EAED);
        title.setText(titleText);

        TermuxSessionsListViewController.alignSessionRowTimesStartWithTitleText(times, timesStartPadding);
        times.setTextColor(0xFFB0B3B8);
        times.setText(timestamp);
        times.setVisibility(View.VISIBLE);
        return row;
    }

    private static TextView buildGroupLabel(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextColor(0xFF8AB4F8);
        label.setTextSize(10f);
        label.setPadding(dpToPx(context, 6), dpToPx(context, 8), dpToPx(context, 6), dpToPx(context, 2));
        return label;
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
            context.getResources().getDisplayMetrics()));
    }

    private static String encodeBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, view.getMeasuredWidth()),
            Math.max(1, view.getMeasuredHeight()), Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF202124);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }
}
