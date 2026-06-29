package com.termux.app.terminal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SessionListHeaderFilterToggleRenderTest {

    private static final int RENDER_WIDTH_PIXELS = 720;

    @Test
    public void rendersShowingAllHeaderWithFilterOffFunnelAndSingleFraction() throws IOException {
        renderHeaderState(false, 3, 22, "session-header-after-showing-all.png");
    }

    @Test
    public void rendersFilteredHeaderWithActiveFunnelAndSingleFraction() throws IOException {
        renderHeaderState(true, 3, 18, "session-header-after-filtered.png");
    }

    private void renderHeaderState(boolean hidingHiddenSessions, int pendingCallSessionCount,
                                   int visibleSessionCount, String fileName) throws IOException {
        View root = inflateActivityLayout();
        TextView titleView = root.findViewById(R.id.session_list_bottom_sheet_title);
        ImageButton toggleButton = root.findViewById(R.id.session_list_bottom_sheet_hidden_toggle_button);
        ViewGroup headerRow = (ViewGroup) titleView.getParent();

        titleView.setText(SessionListBottomSheetController.sessionCountTitle(
            titleView.getContext().getString(R.string.title_session_list_bottom_sheet),
            pendingCallSessionCount, visibleSessionCount));
        toggleButton.setImageResource(
            SessionListBottomSheetController.hiddenToggleIconResource(hidingHiddenSessions));
        toggleButton.setContentDescription(titleView.getContext().getString(
            SessionListBottomSheetController.hiddenToggleContentDescriptionResource(hidingHiddenSessions)));

        Bitmap bitmap = renderToBitmap(headerRow);
        File output = new File(System.getProperty("java.io.tmpdir"), fileName);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        Assert.assertTrue("rendered session header screenshot must be written for device-equivalent proof",
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
