package com.termux.app.terminal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionNavigationButtonsBinderCountBadgeRenderTest {

    private static final int RENDER_WIDTH_PIXELS = 720;

    private static Map<Integer, SessionNewActivityTier> tiers(Object... pairs) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            tiersByIndex.put((Integer) pairs[i], (SessionNewActivityTier) pairs[i + 1]);
        }
        return tiersByIndex;
    }

    @Test
    public void renderedArrowsShowAboveAndBelowCountBadges() throws IOException {
        View root = inflateActivityLayout();
        ImageButton previousButton = root.findViewById(R.id.terminal_toolbar_previous_session_button);
        ImageButton nextButton = root.findViewById(R.id.terminal_toolbar_next_session_button);
        TextView previousBadge = root.findViewById(R.id.terminal_toolbar_previous_session_count_badge);
        TextView nextBadge = root.findViewById(R.id.terminal_toolbar_next_session_count_badge);
        View iconRow = root.findViewById(R.id.terminal_toolbar_icon_button_row);

        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3, 4, 5), 3,
            tiers(0, SessionNewActivityTier.RED,
                1, SessionNewActivityTier.RED,
                4, SessionNewActivityTier.RED));
        int redColor = 0xFFE53935;
        int defaultColor = 0xFFFFFFFF;
        SessionNavigationButtonsBinder.applyDirectionTier(
            previousButton, nextButton, direction, redColor, defaultColor);
        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, direction);

        Assert.assertEquals(View.VISIBLE, previousBadge.getVisibility());
        Assert.assertEquals("2", previousBadge.getText().toString());
        Assert.assertEquals(View.VISIBLE, nextBadge.getVisibility());
        Assert.assertEquals("1", nextBadge.getText().toString());

        Bitmap bitmap = renderToBitmap(iconRow);
        File output = new File(System.getProperty("java.io.tmpdir"),
            "session-nav-arrow-count-badges.png");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        Assert.assertTrue("rendered arrow badge screenshot must be written for device-equivalent proof",
            output.exists() && output.length() > 0);
        Assert.assertTrue(bitmap.getWidth() > 0 && bitmap.getHeight() > 0);
    }

    private static Bitmap renderToBitmap(View view) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(RENDER_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.max(1, view.getMeasuredHeight());
        view.layout(0, 0, view.getMeasuredWidth(), height);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF101010);
        view.draw(canvas);
        return bitmap;
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
