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

/**
 * Proves the colored session activity dot the owner sees in the bottom-sheet session list renders
 * GRAY (not YELLOW) when the displayed {@code out:} time is older than ten minutes, even while raw
 * PTY output keeps arriving.
 *
 * <p>The reported defect: a session whose {@code out:} is 9h old still shows a YELLOW dot. The dot
 * color is rendered by {@link TermuxSessionsListViewController#newActivityIndicatorDrawableRes}
 * (which delegates to {@link SessionActivityTierDrawables#dotDrawableRes}) from the tier produced by
 * {@link TermuxSessionsListViewController#newActivityIndicator}. The tier used to age off the raw
 * last-output-activity time, which {@link SessionNewActivityStore#recordOutputActivity} refreshes on
 * every terminal output change independently of the statusline {@code out} time. So a session with a
 * 9h-old statusline {@code out} but ongoing raw output stayed YELLOW forever. The fix ages the dot
 * tier off the statusline {@code out} timestamp (the same value that produces the displayed
 * {@code out:}), so once it is older than ten minutes the dot is GRAY regardless of raw output.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionActivityDotRenderedColorTest {

    private static final String SESSION_NAME = "agent-session";
    private static final long NOW_MILLIS = 9_000_000_000L;
    private static final long NINE_HOURS_MILLIS = 9L * 60L * 60L * 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;
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

    /**
     * The exact owner-reported scenario: the displayed {@code out:} is 9h old (statusline out time)
     * while raw PTY output keeps arriving (last-output-activity time is fresh). The bottom-sheet row
     * dot rendered through the real production render path MUST be GRAY, not YELLOW.
     */
    @Test
    public void nineHourOldOutWithFreshRawOutputRendersGrayThroughBottomSheetRowRenderPath() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        // Statusline out (the displayed "out:" value) is 9h old.
        store.recordStatuslineTimes(SESSION_NAME, null, NOW_MILLIS - NINE_HOURS_MILLIS, null);
        // Raw PTY output keeps arriving right now — this is what previously kept the dot YELLOW.
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - 1_000L);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);

        SessionNewActivityIndicator indicator =
            TermuxSessionsListViewController.newActivityIndicator(store, SESSION_NAME, NOW_MILLIS);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.GRAY, indicator.getTier());

        int drawableRes =
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(indicator.getTier());
        Assert.assertEquals(R.drawable.ic_session_activity_dot_gray, drawableRes);
        int color = renderedDotColor(drawableRes);
        Assert.assertTrue("expected gray dot but rendered color was " + Integer.toHexString(color),
            closeTo(color, GRAY));
        Assert.assertFalse("rendered dot must not be yellow", closeTo(color, YELLOW));
    }

    /**
     * The same owner-reported divergence asserted on the store-level tier API used by the
     * current-session info path: statusline {@code out} 9h old, raw output fresh, tier MUST be GRAY.
     */
    @Test
    public void nineHourOldOutWithFreshRawOutputResolvesGrayThroughStoreTier() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, null, NOW_MILLIS - NINE_HOURS_MILLIS, null);
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - 1_000L);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);

        SessionNewActivityTier tier = store.tierFor(SESSION_NAME, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);

        int drawableRes = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_gray, drawableRes);
        int color = renderedDotColor(drawableRes);
        Assert.assertTrue("expected gray dot but rendered color was " + Integer.toHexString(color),
            closeTo(color, GRAY));
        Assert.assertFalse("rendered dot must not be yellow", closeTo(color, YELLOW));
    }

    /**
     * A statusline {@code out} within ten minutes must still render YELLOW, even when no raw output
     * has arrived since — the recency dot must keep working for genuinely recent out times.
     */
    @Test
    public void recentStatuslineOutWithinTenMinutesStillRendersYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, null, NOW_MILLIS - ONE_MINUTE_MILLIS, null);

        SessionNewActivityTier tier = store.tierFor(SESSION_NAME, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);

        int drawableRes = TermuxSessionsListViewController.newActivityIndicatorDrawableRes(tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_yellow, drawableRes);
        int color = renderedDotColor(drawableRes);
        Assert.assertTrue("expected yellow dot but rendered color was " + Integer.toHexString(color),
            closeTo(color, YELLOW));
    }

    /**
     * Sessions that only ever emitted raw output (no statusline out) keep aging off the raw output
     * time, so a 9h-old raw-only session is still GRAY — the fallback path must not regress.
     */
    @Test
    public void rawOnlyOldOutputFallsBackToGray() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);

        SessionNewActivityTier tier = store.tierFor(SESSION_NAME, NOW_MILLIS);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
        Assert.assertEquals(R.drawable.ic_session_activity_dot_gray,
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(tier));
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

    /**
     * Renders the real session-row layout for the owner's scenario (9h-old statusline out, fresh raw
     * output) and a recent-out control row, writes a screenshot, and asserts the 9h row's dot is
     * gray and the recent row's dot is yellow.
     */
    @Test
    public void rendersScreenshotProvingNineHourOldOutShowsGrayDotNotYellow() throws Exception {
        ActivityController<android.app.Activity> controller =
            Robolectric.buildActivity(android.app.Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(SESSION_NAME, null, NOW_MILLIS - NINE_HOURS_MILLIS, null);
        store.recordOutputActivity(SESSION_NAME, NOW_MILLIS - 1_000L);
        store.recordSeen(SESSION_NAME, NOW_MILLIS - NINE_HOURS_MILLIS + 1L);
        SessionNewActivityTier grayTier =
            TermuxSessionsListViewController.newActivityIndicator(store, SESSION_NAME, NOW_MILLIS).getTier();
        Assert.assertEquals(SessionNewActivityTier.GRAY, grayTier);

        SessionNewActivityStore recentStore = new SessionNewActivityStore();
        recentStore.recordStatuslineTimes("recent-session", null, NOW_MILLIS - ONE_MINUTE_MILLIS, null);
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
}
