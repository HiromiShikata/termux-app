package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(AndroidJUnit4.class)
public class SessionActivityDotDeviceScreenshotInstrumentedTest {

    private static final String SESSION_NAME = "agent-session";
    private static final long NOW_MILLIS = 9_000_000_000L;
    private static final long NINE_HOURS_MILLIS = 9L * 60L * 60L * 1000L;
    private static final int GRAY = 0xFF9E9E9E;
    private static final int YELLOW = 0xFFFFB300;

    private static boolean closeTo(int actual, int expected) {
        int dr = Math.abs(Color.red(actual) - Color.red(expected));
        int dg = Math.abs(Color.green(actual) - Color.green(expected));
        int db = Math.abs(Color.blue(actual) - Color.blue(expected));
        return Color.alpha(actual) > 200 && dr <= 12 && dg <= 12 && db <= 12;
    }

    private static boolean bandContainsColor(Bitmap bitmap, int top, int bandHeight, int expected) {
        int bottom = Math.min(top + bandHeight, bitmap.getHeight());
        int scanWidth = Math.min(bitmap.getWidth(), 160);
        for (int y = top; y < bottom; y++) {
            for (int x = 0; x < scanWidth; x++) {
                if (closeTo(bitmap.getPixel(x, y), expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static TextView inflateSessionTitleRow(Context context, String text, int dotDrawableRes) {
        View row = View.inflate(context, R.layout.item_terminal_sessions_list, null);
        TextView title = row.findViewById(R.id.session_title);
        title.setText(text);
        title.setTextColor(0xFFE0E0E0);
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(dotDrawableRes, 0, 0, 0);
        title.setCompoundDrawablePadding(20);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        title.measure(widthSpec, heightSpec);
        title.layout(0, 0, title.getMeasuredWidth(), title.getMeasuredHeight());
        return title;
    }

    @Test
    public void nineHourOldOutRendersGrayDotOnDeviceNotYellow() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);
        SessionNewActivityTier grayTier = TermuxSessionsListViewController
            .newActivityIndicator(store, SESSION_NAME, NOW_MILLIS).getTier();
        assertEquals(SessionNewActivityTier.GRAY, grayTier);

        SessionNewActivityStore recentStore = new SessionNewActivityStore();
        recentStore.recordOutputActivity("recent-session", NOW_MILLIS - 60_000L);
        SessionNewActivityTier yellowTier = TermuxSessionsListViewController
            .newActivityIndicator(recentStore, "recent-session", NOW_MILLIS).getTier();
        assertEquals(SessionNewActivityTier.YELLOW, yellowTier);

        TextView yellowRow = inflateSessionTitleRow(context, "recent-session   out 1m ago",
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(yellowTier));
        TextView grayRow = inflateSessionTitleRow(context, "agent-session   out 9h ago",
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(grayTier));

        int width = Math.max(yellowRow.getMeasuredWidth(), grayRow.getMeasuredWidth());
        int rowHeight = Math.max(yellowRow.getMeasuredHeight(), grayRow.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, rowHeight * 2 + 6, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF121212);
        yellowRow.draw(canvas);
        canvas.save();
        canvas.translate(0, rowHeight + 6);
        grayRow.draw(canvas);
        canvas.restore();

        File outDir = context.getExternalFilesDir(null);
        File out = new File(outDir, "session-activity-dot-device-gray-9h.png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        assertTrue(out.exists() && out.length() > 0);

        assertTrue("gray (9h) row must contain a gray dot",
            bandContainsColor(bitmap, rowHeight + 6, rowHeight, GRAY));
        assertFalse("gray (9h) row must NOT contain a yellow dot",
            bandContainsColor(bitmap, rowHeight + 6, rowHeight, YELLOW));
        assertTrue("recent (1m) row must contain a yellow dot",
            bandContainsColor(bitmap, 0, rowHeight, YELLOW));
    }
}
