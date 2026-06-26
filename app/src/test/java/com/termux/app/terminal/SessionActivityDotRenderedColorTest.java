package com.termux.app.terminal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionActivityDotRenderedColorTest {

    private static final String SESSION_NAME = "agent-session";
    private static final long NOW_MILLIS = 9_000_000_000L;
    private static final long NINE_HOURS_MILLIS = 9L * 60L * 60L * 1000L;
    private static final int GRAY = 0xFF9E9E9E;
    private static final int YELLOW = 0xFFFFB300;

    private static int renderedDotColor(int drawableRes) {
        Context context = RuntimeEnvironment.getApplication();
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        Assert.assertNotNull(drawable);
        int width = Math.max(drawable.getIntrinsicWidth(), 1);
        int height = Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap.getPixel(width / 2, height / 2);
    }

    private static boolean bandContainsColor(Bitmap bitmap, int top, int bandHeight, int expected) {
        int bottom = Math.min(top + bandHeight, bitmap.getHeight());
        int scanWidth = Math.min(bitmap.getWidth(), 120);
        for (int y = top; y < bottom; y++) {
            for (int x = 0; x < scanWidth; x++) {
                if (closeTo(bitmap.getPixel(x, y), expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean closeTo(int actual, int expected) {
        int dr = Math.abs(Color.red(actual) - Color.red(expected));
        int dg = Math.abs(Color.green(actual) - Color.green(expected));
        int db = Math.abs(Color.blue(actual) - Color.blue(expected));
        return Color.alpha(actual) > 200 && dr <= 8 && dg <= 8 && db <= 8;
    }

    @Test
    public void nineHourOldOutResolvesToGrayThroughTheBottomSheetRowRenderPath() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);

        SessionNewActivityIndicator indicator =
            TermuxSessionsListViewController.newActivityIndicator(store, SESSION_NAME, NOW_MILLIS);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.GRAY, indicator.getTier());

        int drawableRes =
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.getTier());
        int color = renderedDotColor(drawableRes);
        Assert.assertTrue("expected gray dot but rendered color was " + Integer.toHexString(color),
            closeTo(color, GRAY));
        Assert.assertFalse("rendered dot must not be yellow", closeTo(color, YELLOW));
    }

    @Test
    public void nineHourOldOutResolvesToGrayThroughTheCurrentSessionInfoTierPath() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);

        SessionNewActivityTier tier = store.tierFor(SESSION_NAME, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);

        int drawableRes = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(tier);
        int color = renderedDotColor(drawableRes);
        Assert.assertTrue("expected gray dot but rendered color was " + Integer.toHexString(color),
            closeTo(color, GRAY));
        Assert.assertFalse("rendered dot must not be yellow", closeTo(color, YELLOW));
    }

    private static TextView buildSessionTitleRow(Context context, String text, int dotDrawableRes) {
        View row = View.inflate(context, R.layout.item_terminal_sessions_list, null);
        TextView title = row.findViewById(R.id.session_title);
        title.setText(text);
        title.setTextColor(0xFFFFFFFF);
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(dotDrawableRes, 0, 0, 0);
        title.setCompoundDrawablePadding(16);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        title.measure(widthSpec, heightSpec);
        title.layout(0, 0, title.getMeasuredWidth(), title.getMeasuredHeight());
        return title;
    }

    @Test
    public void rendersScreenshotProvingNineHourOldOutShowsGrayDotNotYellow() throws Exception {
        ActivityController<android.app.Activity> controller =
            Robolectric.buildActivity(android.app.Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);
        SessionNewActivityTier grayTier =
            TermuxSessionsListViewController.newActivityIndicator(store, SESSION_NAME, NOW_MILLIS).getTier();
        Assert.assertEquals(SessionNewActivityTier.GRAY, grayTier);

        SessionNewActivityStore recentStore = new SessionNewActivityStore();
        recentStore.recordOutputActivity("recent-session", NOW_MILLIS - 60_000L);
        SessionNewActivityTier yellowTier =
            TermuxSessionsListViewController.newActivityIndicator(recentStore, "recent-session", NOW_MILLIS).getTier();
        Assert.assertEquals(SessionNewActivityTier.YELLOW, yellowTier);

        TextView yellowRow = buildSessionTitleRow(context, "recent-session   out 1m ago",
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(yellowTier));
        TextView grayRow = buildSessionTitleRow(context, "agent-session   out 9h ago",
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(grayTier));

        int width = Math.max(yellowRow.getMeasuredWidth(), grayRow.getMeasuredWidth());
        int rowHeight = Math.max(yellowRow.getMeasuredHeight(), grayRow.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, rowHeight * 2 + 4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF121212);
        yellowRow.draw(canvas);
        canvas.save();
        canvas.translate(0, rowHeight + 4);
        grayRow.draw(canvas);
        canvas.restore();

        File out = new File(System.getProperty("session.dot.screenshot.dir", "build"),
            "session-activity-dot-gray-9h.png");
        out.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        Assert.assertTrue(out.exists() && out.length() > 0);

        Assert.assertTrue("gray row must contain a gray dot",
            bandContainsColor(bitmap, rowHeight + 4, rowHeight, GRAY));
        Assert.assertFalse("gray row must not contain a yellow dot",
            bandContainsColor(bitmap, rowHeight + 4, rowHeight, YELLOW));
        Assert.assertTrue("recent row must contain a yellow dot",
            bandContainsColor(bitmap, 0, rowHeight, YELLOW));
        controller.destroy();
    }

    @Test
    public void recentOutWithinTenMinutesStillRendersYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - 60_000L);

        SessionNewActivityTier tier = store.tierFor(SESSION_NAME, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);

        int drawableRes = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(tier);
        int color = renderedDotColor(drawableRes);
        Assert.assertTrue("expected yellow dot but rendered color was " + Integer.toHexString(color),
            closeTo(color, YELLOW));
    }
}
