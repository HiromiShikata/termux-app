package com.termux.app.terminal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.text.Layout;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BottomSheetRowReplyRenderInstrumentedTest {

    @Test
    public void rowRendersReplyTokenWhenSessionNameIsLongAtNarrowWidth() {
        Context baseContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context context = new ContextThemeWrapper(baseContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionNewActivityStore store = new SessionNewActivityStore();
        String sessionName = "demo-long-session-name-0123456789-worker";
        String timestamp = TermuxSessionsListViewController.buildTimestampLine(store, sessionName, 61_000L);

        SessionInfoBlock titleBlock = SessionInfoBlock.compose("", sessionName, "",
            "definition title here", "claude session subtitle");

        AtomicBoolean replyVisible = new AtomicBoolean(false);
        AtomicReference<String> renderedTimes = new AtomicReference<>("");
        AtomicReference<String> screenshotBase64 = new AtomicReference<>("");

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            float density = context.getResources().getDisplayMetrics().density;
            View row = LayoutInflater.from(context)
                .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);
            TextView title = row.findViewById(R.id.session_title);
            TextView times = row.findViewById(R.id.session_row_times);
            assertNotNull(title);
            assertNotNull(times);

            title.setText(titleBlock.text());
            times.setText(timestamp);
            times.setVisibility(View.VISIBLE);

            int widthPx = (int) (140 * density);
            int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            row.measure(widthSpec, heightSpec);
            row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());

            String visibleTimes = visibleText(times);
            renderedTimes.set(visibleTimes);
            replyVisible.set(visibleTimes.contains("reply:"));

            screenshotBase64.set(encodeBitmap(row));
        });

        System.out.println("AFTER_FIX widthDp=140 renderedTimes=[" + renderedTimes.get()
            + "] containsReply=" + replyVisible.get());
        String base64 = screenshotBase64.get();
        int chunk = 2000;
        for (int i = 0; i < base64.length(); i += chunk) {
            System.out.println("SHOT_B64 " + base64.substring(i, Math.min(base64.length(), i + chunk)));
        }
        System.out.println("SHOT_B64_END");

        assertTrue("dedicated row times view must render the reply token at a narrow width with a long "
                + "session name; renderedTimes=[" + renderedTimes.get() + "]",
            replyVisible.get());
    }

    @Test
    public void sharedTitleViewTruncatesReplyWhenSessionNameIsLongAtNarrowWidth() {
        Context baseContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context context = new ContextThemeWrapper(baseContext,
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionNewActivityStore store = new SessionNewActivityStore();
        String sessionName = "demo-long-session-name-0123456789-worker";
        String timestamp = TermuxSessionsListViewController.buildTimestampLine(store, sessionName, 61_000L);

        SessionInfoBlock sharedBlock = SessionInfoBlock.compose("", sessionName, timestamp,
            "definition title here", "claude session subtitle");

        AtomicBoolean replyVisible = new AtomicBoolean(true);
        AtomicReference<String> renderedTitle = new AtomicReference<>("");
        AtomicReference<String> screenshotBase64 = new AtomicReference<>("");

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            float density = context.getResources().getDisplayMetrics().density;
            View row = LayoutInflater.from(context)
                .inflate(R.layout.item_terminal_sessions_list, new FrameLayout(context), false);
            TextView title = row.findViewById(R.id.session_title);
            assertNotNull(title);
            title.setText(sharedBlock.text());

            int widthPx = (int) (140 * density);
            int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            row.measure(widthSpec, heightSpec);
            row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());

            String visibleTitle = visibleText(title);
            renderedTitle.set(visibleTitle);
            replyVisible.set(visibleTitle.contains("reply:"));

            screenshotBase64.set(encodeBitmap(row));
        });

        System.out.println("BEFORE_SIM widthDp=140 renderedTitle=[" + renderedTitle.get()
            + "] containsReply=" + replyVisible.get());
        String base64 = screenshotBase64.get();
        int chunk = 2000;
        for (int i = 0; i < base64.length(); i += chunk) {
            System.out.println("SHOT_BEFORE_B64 " + base64.substring(i, Math.min(base64.length(), i + chunk)));
        }
        System.out.println("SHOT_BEFORE_B64_END");

        assertTrue("the pre-fix shared title path is expected to drop the reply token at a narrow width "
                + "with a long session name; renderedTitle=[" + renderedTitle.get() + "]",
            !replyVisible.get());
    }

    private static String visibleText(TextView textView) {
        Layout layout = textView.getLayout();
        if (layout == null) {
            return textView.getText().toString();
        }
        StringBuilder visible = new StringBuilder();
        int lines = Math.min(layout.getLineCount(), textView.getMaxLines());
        for (int i = 0; i < lines; i++) {
            int start = layout.getLineStart(i);
            int end = layout.getLineEnd(i);
            int ellipsis = layout.getEllipsisCount(i);
            int lineEnd = ellipsis > 0 ? start + layout.getEllipsisStart(i) : end;
            visible.append(textView.getText().subSequence(start, Math.min(lineEnd, textView.getText().length())));
        }
        return visible.toString();
    }

    private static String encodeBitmap(View row) {
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, row.getMeasuredWidth()),
            Math.max(1, row.getMeasuredHeight()), Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFF202124);
        Canvas canvas = new Canvas(bitmap);
        row.draw(canvas);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }
}
