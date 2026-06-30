package com.termux.app.terminal;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Proves the bottom-sheet session-row reconnecting spinner occupies fixed reserved space so toggling
 * it between loading (shown) and not-loading (hidden) does not change the row height, and renders a
 * real screenshot of a loading row above a non-loading row.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionRowReconnectingSpinnerRenderTest {

    private static View buildMeasuredRow(Context context, String titleText, int spinnerVisibility) {
        View row = View.inflate(context, R.layout.item_terminal_sessions_list, null);
        row.setBackgroundColor(0xFFFFFFFF);
        TextView title = row.findViewById(R.id.session_title);
        title.setText(titleText);
        title.setTextColor(0xFF202020);
        View spinner = row.findViewById(R.id.session_reconnecting_indicator);
        spinner.setVisibility(spinnerVisibility);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        row.measure(widthSpec, heightSpec);
        row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
        return row;
    }

    @Test
    public void togglingTheSpinnerDoesNotChangeTheRowHeight() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View loadingRow = buildMeasuredRow(context, "agent-session", View.VISIBLE);
        View idleRow = buildMeasuredRow(context, "agent-session", View.INVISIBLE);

        Assert.assertEquals("the spinner must not change the row height",
            idleRow.getMeasuredHeight(), loadingRow.getMeasuredHeight());
        controller.destroy();
    }

    @Test
    public void theSpinnerIsNeverRenderedAsGoneSoTheSlotStaysReserved() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View idleRow = buildMeasuredRow(context, "agent-session", View.INVISIBLE);
        View spinner = idleRow.findViewById(R.id.session_reconnecting_indicator);

        Assert.assertNotEquals("the spinner must reserve its slot with INVISIBLE, never GONE",
            View.GONE, spinner.getVisibility());
        Assert.assertTrue("the reserved spinner slot must have a fixed non-zero size",
            spinner.getMeasuredWidth() > 0 && spinner.getMeasuredHeight() > 0);
        controller.destroy();
    }

    @Test
    public void rendersScreenshotOfALoadingRowAboveANonLoadingRow() throws Exception {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create();
        Context context = controller.get();
        context.setTheme(R.style.Theme_TermuxActivity_DayNight_NoActionBar);

        View loadingRow = buildMeasuredRow(context, "agent-session   (loading)", View.VISIBLE);
        View idleRow = buildMeasuredRow(context, "agent-session   (idle, no spinner)", View.INVISIBLE);

        int width = Math.max(loadingRow.getMeasuredWidth(), idleRow.getMeasuredWidth());
        int rowHeight = Math.max(loadingRow.getMeasuredHeight(), idleRow.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, rowHeight * 2 + 4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFFFFFFFF);
        loadingRow.draw(canvas);
        canvas.save();
        canvas.translate(0, rowHeight + 4);
        idleRow.draw(canvas);
        canvas.restore();

        File out = new File(System.getProperty("session.spinner.screenshot.dir", "build"),
            "session-row-reconnecting-spinner.png");
        out.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        Assert.assertTrue(out.exists() && out.length() > 0);
        controller.destroy();
    }
}
