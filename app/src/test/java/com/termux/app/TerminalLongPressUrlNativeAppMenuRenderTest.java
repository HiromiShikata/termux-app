package com.termux.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TerminalLongPressUrlNativeAppMenuRenderTest {

    private static final int RENDER_WIDTH_PIXELS = 720;

    @Test
    public void rendersTerminalLongPressMenuWithOpenInGoogleCalendarForCalendarUrl() throws IOException {
        Context context = RuntimeEnvironment.getApplication();
        String calendarUrl = "https://calendar.google.com/calendar/u/0/r/eventedit/abc123";

        List<TermuxActivity.LongPressedUrlMenuItem> items =
            TermuxActivity.longPressedUrlMenuItems(context, calendarUrl);

        String openInCalendarTitle =
            context.getString(R.string.action_open_link_in_native_app, "Google Calendar");
        Assert.assertTrue("terminal long-press menu must contain the open-in-Google-Calendar option",
            containsTitle(items, openInCalendarTitle));

        View menuView = buildMenuView(context, calendarUrl, items);
        Bitmap bitmap = renderToBitmap(menuView);
        File output = new File(System.getProperty("java.io.tmpdir"),
            "terminal-longpress-open-in-google-calendar.png");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        Assert.assertTrue("rendered terminal long-press menu screenshot must be written",
            output.exists() && output.length() > 0);
        Assert.assertTrue(bitmap.getWidth() > 0 && bitmap.getHeight() > 0);
    }

    private static boolean containsTitle(List<TermuxActivity.LongPressedUrlMenuItem> items,
                                         String title) {
        for (TermuxActivity.LongPressedUrlMenuItem item : items) {
            if (title.contentEquals(item.getTitle())) return true;
        }
        return false;
    }

    private static View buildMenuView(Context context, String url,
                                      List<TermuxActivity.LongPressedUrlMenuItem> items) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#212121"));
        container.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));

        TextView urlTitle = new TextView(context);
        urlTitle.setText(url);
        urlTitle.setTextColor(Color.parseColor("#9E9E9E"));
        urlTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        urlTitle.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 12));
        container.addView(urlTitle, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (TermuxActivity.LongPressedUrlMenuItem item : items) {
            TextView entry = new TextView(context);
            entry.setText(item.getTitle());
            entry.setTextColor(Color.WHITE);
            entry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            entry.setGravity(Gravity.CENTER_VERTICAL);
            entry.setPadding(dp(context, 12), dp(context, 14), dp(context, 12), dp(context, 14));
            container.addView(entry, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return container;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Bitmap renderToBitmap(View view) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(RENDER_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.max(1, view.getMeasuredHeight());
        view.layout(0, 0, view.getMeasuredWidth(), height);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#212121"));
        view.draw(canvas);
        return bitmap;
    }
}
